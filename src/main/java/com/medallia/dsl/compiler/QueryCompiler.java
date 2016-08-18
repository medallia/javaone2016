package com.medallia.dsl.compiler;

import com.medallia.codegen.JavaCodeGenerator;
import com.medallia.codegen.SimpleJavaCompiler;
import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
import com.medallia.data.Segment;
import com.medallia.dsl.Aggregate;
import com.medallia.dsl.Aggregate.StatsAggregate;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.nodes.AndExpr;
import com.medallia.dsl.nodes.ExprVisitor;
import com.medallia.dsl.nodes.InExpr;
import com.medallia.dsl.nodes.NotExpr;
import com.medallia.dsl.nodes.OrExpr;

import java.util.stream.LongStream;

public class QueryCompiler<T> {
	private final Query<T> query;
	private final DataSet dataSet;

	public QueryCompiler(Query<T> query, DataSet dataSet) {
		this.query = query;
		this.dataSet = dataSet;
	}

	@SuppressWarnings("unchecked")
	public Class<? extends CompiledQueryBase<T>> compile() {
		return (Class<? extends CompiledQueryBase<T>>) SimpleJavaCompiler.compile(CompiledQueryBase.class, fileCg -> {
			fileCg.generateImport(CompiledQueryBase.class);
			fileCg.generateImport(Segment.class);
			fileCg.generateImport(FieldStats.class);

			fileCg.publicClass("CompiledQuery")
					.extend("CompiledQueryBase")
					.build(classCg -> {
						classCg.publicMethod("void", "process").arg("Segment", "segment")
								.build(methodCg -> {
									methodCg.declare("long[][]", "rawData", "segment.rawData");
									methodCg.declare("int", "nRows", "rawData[0].length");
									methodCg.boundedLoop("row", "0", "nRows", loopCg -> {
										loopCg.ifThen(this::generateFilter, this::generateAggregate);
									});
								});
					});

		});
	}

	private void generateFilter(JavaCodeGenerator cg) {
		cg.print(query.buildExpressionTree().visit(new ExprVisitor<String>() {
			@Override
			public String visit(AndExpr andExpr) {
				return andExpr.getLeft().visit(this) + " && " + andExpr.getRight().visit(this);
			}

			@Override
			public String visit(InExpr inExpr) {
				FieldDefinition field = dataSet.getFieldByName(inExpr.getFieldName());
				return "(" + LongStream.of(inExpr.getValues())
						.mapToObj(v -> String.format("rawData[%d][row] == %dL", field.getColumn(), v))
						.reduce((a,b) -> a + " || " + b)
						.orElseThrow(() -> new RuntimeException("empty filter"))
						+ ")";
			}

			@Override
			public String visit(NotExpr notExpr) {
				return "!" + notExpr.getTarget().visit(this);
			}

			@Override
			public String visit(OrExpr orExpr) {
				return orExpr.getLeft().visit(this) + " || " + orExpr.getRight().visit(this);
			}
		}));
	}

	private void generateAggregate(JavaCodeGenerator cg) {
		 if (query.aggregateOp instanceof Aggregate.StatsAggregate) {
		 	 final StatsAggregate statsAggregate = (StatsAggregate) query.aggregateOp;
			 cg.ifThen("result == null", ifCg -> ifCg.println("result = new FieldStats();")); // TODO: hack
			 cg.println("((FieldStats)result).count++;");
			 cg.printf("((FieldStats)result).sum += rawData[%d][row];%n", dataSet.getFieldByName(statsAggregate.getFieldName()).getColumn());

		 } else {
		 	throw new UnsupportedOperationException("not implemented");
		 }
	}
}
