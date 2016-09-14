package com.medallia.dsl.compiler;

import com.medallia.codegen.JavaCodeGenerator;
import com.medallia.codegen.SimpleJavaCompiler;
import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;

public class QueryCompiler<T> {
	protected final Query<T> query;
	protected final DataSet dataSet;

	public QueryCompiler(Query<T> query, DataSet dataSet) {
		this.query = query;
		this.dataSet = dataSet;
	}


	@SuppressWarnings("unchecked")
	public Supplier<CompiledQueryBase<T>>  compile() {
		final Agg<T> aggregate = query.aggregateOp.buildAgg();

		Class<? extends CompiledQueryBase<T>> compiled = (Class<? extends CompiledQueryBase<T>>)
				SimpleJavaCompiler.compile(CompiledQueryBase.class, fileCg -> {
			fileCg.generateImport(CompiledQueryBase.class);
			fileCg.generateImport(Segment.class);
			fileCg.generateImport(FieldStats.class);

			fileCg.publicClass("CompiledQuery")
					.extend("CompiledQueryBase")
					.build(classCg -> {
						classCg.declareExisting("result");

						classCg.println("public CompiledQuery(Object result)");
						classCg.beginBlock();
						classCg.println("this.result = result;");
						classCg.endBlock();

						classCg.publicMethod("void", "process").arg("Segment", "segment")
								.build(methodCg -> {
									methodCg.declare("long[][]", "rawData", "segment.rawData");
									methodCg.declare("int", "nRows", "rawData[0].length");
									final String resultType = resultType(aggregate);
									methodCg.declare(resultType, "result", "(%s)this.result", resultType);
									methodCg.boundedLoop("row", "0", "nRows", loopCg -> {
										loopCg.ifThen(this::generateFilter, this::generateAggregate);
									});
								});
					});

		});
		Constructor<? extends CompiledQueryBase<T>> constructor;
		try {
			constructor = compiled.getConstructor(Object.class);
		} catch (NoSuchMethodException e) {
			throw new Error("Cannot find constructor", e);
		}
		return () -> {
			try {
				return constructor.newInstance(aggregate.makeResult(dataSet));
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("unexpected exception instantiating query", e);
			}
		};
	}

	protected void generateFilter(JavaCodeGenerator cg) {
		cg.print(query.buildExpressionTree().visit(new ExprVisitor<String>() {
			@Override
			public String visit(AndExpr andExpr) {
				return "(" + andExpr.getLeft().visit(this) + " && " + andExpr.getRight().visit(this) + ")";
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
				return "(" + orExpr.getLeft().visit(this) + " || " + orExpr.getRight().visit(this) + ")";
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
					cg.println(cg.variable("result") + ".count++;");
					cg.printf("%s.sum += rawData[%d][row];%n", cg.variable("result"), dataSet.getFieldByName(statsAggregate.getFieldName()).getColumn());
				};
			}

		});

		generator.accept(cg);
	}

	private String resultType(Agg<?> agg) {
		return agg.visit(statsAggregate -> "FieldStats");
	}
}
