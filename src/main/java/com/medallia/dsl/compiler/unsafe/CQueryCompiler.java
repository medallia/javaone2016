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
import com.medallia.dsl.ast.InExpr;
import com.medallia.dsl.ast.StatsAggregate;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.unsafe.Driver;
import com.medallia.unsafe.NativeModule;

import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Query compiler that generates C++ code for the query.
 * This one for simplicity is not as generic as the other compilers and expects a {@link FieldStats} as the query result.
 * @param <T> query result type.
 */
public class CQueryCompiler<T extends FieldStats> extends BranchReducingQueryCompiler<T> {

	public CQueryCompiler(Query<T> query, DataSet dataSet) {
		super(query, dataSet);
	}

	@SuppressWarnings("unchecked")
	public Supplier<CompiledQueryBase<T>> compile() {
		final Agg<T> aggregate = query.aggregateOp.buildAgg();
		// This is an abuse, but Java and C/C++ are similar enough that we can us it for both with some care
		final JavaCodeGenerator cg = new JavaCodeGenerator();

		cg.println("#include <jni.h>");

		// Store field ids to amortize lookup time
		cg.println("static jfieldID rawDataFld;");
		cg.println("static jfieldID resultFld;");
		cg.println("static jfieldID sumFld;");
		cg.println("static jfieldID countFld;");

		cg.packageMethod("void", "init").arg("JNIEnv*", "env").arg("jobject", "self")
				.build(methodCg -> {
					methodCg.declare("jclass", "segmentClass", "env->FindClass(\"%s\")", Segment.class.getCanonicalName().replace('.', '/'));
					methodCg.println("rawDataFld = env->GetFieldID(segmentClass, \"rawData\", \"[[I\");");
					methodCg.declare("jclass", "compiledQueryClass", "env->FindClass(\"%s\")", CCompiledQuery.class.getCanonicalName().replace('.', '/'));
					methodCg.println("resultFld = env->GetFieldID(compiledQueryClass, \"result\", \"L" + Object.class.getCanonicalName().replace('.', '/') + ";\");");
					methodCg.declare("jclass", "fieldStatsClass", "env->FindClass(\"%s\")", FieldStats.class.getCanonicalName().replace('.', '/'));
					methodCg.println("sumFld = env->GetFieldID(fieldStatsClass, \"sum\", \"D\");");
					methodCg.println("countFld = env->GetFieldID(fieldStatsClass, \"count\", \"J\");");
				});

		// Generate the process method
		cg.packageMethod("void", "process").arg("JNIEnv*", "env").arg("jobject", "self").arg("jobject", "segment")
				.build(methodCg -> {
					methodCg.declare("jobjectArray", "rawDataObj", "(jobjectArray) env->GetObjectField(segment, rawDataFld)");
					methodCg.declare("jint", "cols", "env->GetArrayLength(rawDataObj)");
					methodCg.println("jint* rawData[cols];");
					methodCg.println("jobject arrays[cols];");
					methodCg.declare("jint", "nRows", "0");
					// Extract column arrays
					methodCg.boundedLoop("i", "0", "(int)cols", loopCg -> {
						loopCg.println("arrays[i] = env->GetObjectArrayElement(rawDataObj, i);");
						loopCg.println("nRows = env->GetArrayLength((jarray)arrays[i]);");
					});
					methodCg.boundedLoop("i", "0", "(int)cols", loopCg -> {
						loopCg.println("rawData[i] = (jint*)env->GetPrimitiveArrayCritical((jarray)arrays[i], 0);");
					});

					// Inner query loop. This assumes we'll be returning a FieldStats object
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
		// Initialize the native module (setup field ids)
		new CCompiledQuery(nativeModule, null).init();

		return () -> new CCompiledQuery(nativeModule, aggregate.makeResult(dataSet));
	}

	private void generateAggregate(JavaCodeGenerator cg) {
		// This can be done with lambdas, but it's easier to read this way
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
}
