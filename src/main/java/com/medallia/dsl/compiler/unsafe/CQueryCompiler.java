package com.medallia.dsl.compiler.unsafe;

import com.medallia.codegen.JavaCodeGenerator;
import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
import com.medallia.data.FieldSpec;
import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.ast.Agg;
import com.medallia.dsl.ast.AggVisitor;
import com.medallia.dsl.ast.AndExpr;
import com.medallia.dsl.ast.ConstantExpr;
import com.medallia.dsl.ast.ExprVisitor;
import com.medallia.dsl.ast.InExpr;
import com.medallia.dsl.ast.NotExpr;
import com.medallia.dsl.ast.OrExpr;
import com.medallia.dsl.ast.StatsAggregate;
import com.medallia.unsafe.Driver;
import com.medallia.unsafe.NativeModule;

import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class CQueryCompiler<T> {
	protected final Query<T> query;
	protected final DataSet dataSet;

	public CQueryCompiler(Query<T> query, DataSet dataSet) {
		this.query = query;
		this.dataSet = dataSet;
	}


	@SuppressWarnings("unchecked")
	public Supplier<CCompiledQuery> compile() {
		final Agg<T> aggregate = query.aggregateOp.buildAgg();
		// This is an abuse, but Java and C/C++ are similar enough that we can us it for both with some care
		final JavaCodeGenerator cg = new JavaCodeGenerator();

		cg.println("#include <jni.h>");
		cg.println("#include <chrono>");

		cg.println("static jfieldID rawDataFld;");
		cg.println("static jfieldID resultFld;");
		cg.println("static jfieldID sumFld;");
		cg.println("static jfieldID countFld;");
		cg.packageMethod("void", "init").arg("JNIEnv*", "env").arg("jobject", "self")
				.build(methodCg -> {
					methodCg.declare("jclass", "segmentClass", "env->FindClass(\"%s\")", Segment.class.getCanonicalName().replace('.', '/'));
					methodCg.println("rawDataFld = env->GetFieldID(segmentClass, \"rawData\", \"[[J\");");
					methodCg.declare("jclass", "compiledQueryClass", "env->FindClass(\"%s\")", CCompiledQuery.class.getCanonicalName().replace('.', '/'));
					methodCg.println("resultFld = env->GetFieldID(compiledQueryClass, \"result\", \"L" + FieldStats.class.getCanonicalName().replace('.', '/') + ";\");");
					methodCg.declare("jclass", "fieldStatsClass", "env->FindClass(\"%s\")", FieldStats.class.getCanonicalName().replace('.', '/'));
					methodCg.println("sumFld = env->GetFieldID(fieldStatsClass, \"sum\", \"D\");");
					methodCg.println("countFld = env->GetFieldID(fieldStatsClass, \"count\", \"J\");");
				});

		cg.packageMethod("void", "process").arg("JNIEnv*", "env").arg("jobject", "self").arg("jobject", "segment")
				.build(methodCg -> {
					methodCg.declare("jobjectArray", "rawDataObj", "(jobjectArray) env->GetObjectField(segment, rawDataFld)");
					methodCg.declare("jint", "cols", "env->GetArrayLength(rawDataObj)");
					methodCg.println("jlong* rawData[cols];");
					methodCg.println("jobject arrays[cols];");
					methodCg.declare("jint", "nRows", "0");
					// Extract column arrays
					methodCg.boundedLoop("i", "0", "(int)cols", loopCg -> {
						loopCg.println("arrays[i] = env->GetObjectArrayElement(rawDataObj, i);");
						loopCg.println("nRows = env->GetArrayLength((jarray)arrays[i]);");
					});
					methodCg.boundedLoop("i", "0", "(int)cols", loopCg -> {
						loopCg.println("rawData[i] = (jlong*)env->GetPrimitiveArrayCritical((jarray)arrays[i], 0);");
					});

					methodCg.declare("jdouble", "sum", "0");
					methodCg.declare("jlong", "count", "0");
					methodCg.boundedLoop("row", "0", "(int)nRows", loopCg -> {
						loopCg.ifThen(this::generateFilter, this::generateAggregate);
					});

					// Release column arrays
					methodCg.boundedLoop("i", "0", "(int)cols", loopCg -> {
						loopCg.println("env->ReleasePrimitiveArrayCritical((jarray)arrays[i], rawData[i], 0);");
					});

					// Update result
					methodCg.declare("jobject", "result", "env->GetObjectField(self, resultFld)");
					methodCg.println("env->SetLongField(result, countFld, env->GetLongField(result, countFld) + count);");
					methodCg.println("env->SetDoubleField(result, sumFld, env->GetDoubleField(result, sumFld) + sum);");

				});

		StringWriter sw = new StringWriter();
		cg.generate(sw);
		System.out.println(sw);
		final NativeModule nativeModule = Driver.compileInMemory(sw.toString());
		System.out.println(nativeModule.getErrors());
		// Initialize the module
		new CCompiledQuery(nativeModule).init();

		return () -> new CCompiledQuery(nativeModule);
	}

	private void generateFilter(JavaCodeGenerator cg) {
		cg.print(query.buildExpressionTree().visit(new ExprVisitor<String>() {
			@Override
			public String visit(AndExpr andExpr) {
				return andExpr.getLeft().visit(this) + " && " + andExpr.getRight().visit(this);
			}

			@Override
			public String visit(InExpr inExpr) {
				return generateInExpr(cg, inExpr);
			}

			@Override
			public String visit(NotExpr notExpr) {
				return "!(" + notExpr.getTarget().visit(this) + ")";
			}

			@Override
			public String visit(OrExpr orExpr) {
				return orExpr.getLeft().visit(this) + " || " + orExpr.getRight().visit(this);
			}

			@Override
			public String visit(ConstantExpr constantExpr) {
				return String.valueOf(constantExpr.value);
			}
		}));
	}

	protected String generateInExpr(JavaCodeGenerator cg, InExpr inExpr) {
		FieldDefinition field = dataSet.getFieldByName(inExpr.getFieldName());
		return "(" + LongStream.of(inExpr.getValues())
				.mapToObj(v -> String.format("rawData[%d][row] == %dL", field.getColumn(), v))
				.reduce((a,b) -> a + " || " + b)
				.orElseThrow(() -> new RuntimeException("empty filter"))
				+ ")";
	}

	private void generateAggregate(JavaCodeGenerator cg) {
		Consumer<JavaCodeGenerator> generator = query.aggregateOp.buildAgg().visit(new AggVisitor<Consumer<JavaCodeGenerator>>() {
			@Override
			public Consumer<JavaCodeGenerator> visit(StatsAggregate statsAggregate) {
				return cg -> {
					cg.println(cg.variable("count") + "++;");
					cg.printf("%s += rawData[%d][row];%n", cg.variable("sum"), dataSet.getFieldByName(statsAggregate.getFieldName()).getColumn());
				};
			}

		});

		generator.accept(cg);
	}

	private String resultType(Agg<?> agg) {
		return agg.visit(statsAggregate -> "FieldStats");
	}

	public static void main(String[] args) {
		DataSet dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("c", 0, 11),
				new FieldSpec("d", 0, 5),
				new FieldSpec("e", 0, 20),
				new FieldSpec("f", 0, 5),
				new FieldSpec("ltr", 0, 11));

		final Query<FieldStats> query = newQuery()
				.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
				.aggregate(statsAggregate("ltr"));

		CQueryCompiler<FieldStats> compiler = new CQueryCompiler<>(query, dataSet);

		compiler.compile();
	}
}
