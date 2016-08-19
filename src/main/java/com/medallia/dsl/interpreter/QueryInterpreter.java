package com.medallia.dsl.interpreter;

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
import com.medallia.dsl.ast.DistributionAggregate;
import com.medallia.dsl.ast.Expr;
import com.medallia.dsl.ast.ExprVisitor;
import com.medallia.dsl.ast.InExpr;
import com.medallia.dsl.ast.NotExpr;
import com.medallia.dsl.ast.OrExpr;
import com.medallia.dsl.ast.StatsAggregate;

import java.util.stream.LongStream;

public class QueryInterpreter<T> {

	private final Query<T> query;

	public QueryInterpreter(Query<T> query) {
		this.query = query;
	}

	public T eval(DataSet dataSet) {
		final Expr expr = query.buildExpressionTree();
		final Agg<T> agg = query.aggregateOp.buildAgg();
		final T result = agg.makeResult(dataSet);
		final Aggregator aggregator = makeAggregator(dataSet, agg);

		for (Segment segment : dataSet.getSegments()) {
			final long[][] rawData = segment.rawData;
			final int nRows = rawData[0].length;
			for (int row = 0; row < nRows; row++) {
				if (eval(expr, dataSet, segment, row)) {
					aggregator.process(segment, row, result);
				}
			}
		}

		return result;
	}

	private boolean eval(Expr expression, DataSet dataSet, Segment segment, int row) {
		return expression.visit(new ExprVisitor<Boolean>() {
			@Override
			public Boolean visit(AndExpr andExpr) {
				return andExpr.getLeft().visit(this) && andExpr.getRight().visit(this);
			}

			@Override
			public Boolean visit(InExpr inExpr) {
				int column = dataSet.getFieldByName(inExpr.getFieldName()).getColumn();
				long[] values = inExpr.getValues();
				for (long value : values) {
					if (value == segment.rawData[column][row])
						return true;
				}
				return false;
			}

			@Override
			public Boolean visit(NotExpr notExpr) {
				return !notExpr.getTarget().visit(this);
			}

			@Override
			public Boolean visit(OrExpr orExpr) {
				return orExpr.getLeft().visit(this) || orExpr.getRight().visit(this);
			}

			@Override
			public Boolean visit(ConstantExpr constantExpr) {
				return constantExpr.value;
			}
		});
	}

	static Aggregator makeAggregator(DataSet dataSet, Agg<?> agg) {
		return agg.visit(new AggVisitor<Aggregator>() {
			@Override
			public Aggregator visit(StatsAggregate statsAggregate) {
				int column = dataSet.getFieldByName(statsAggregate.getFieldName())
						.getColumn();
				return (segment, row, result) -> {
					((FieldStats)result).count++;
					((FieldStats)result).sum += segment.rawData[column][row];
				};
			}

			@Override
			public Aggregator visit(DistributionAggregate<?> distributionAggregate) {
				FieldDefinition fieldDef = dataSet.getFieldByName(distributionAggregate.getFieldName());
				FieldSpec fieldSpec = fieldDef.getFieldSpec();

				// Field information for the lambda
				long origin = fieldSpec.getOrigin(), bound = fieldSpec.getBound();
				int column = fieldDef.getColumn();

				// Build an interpreter array
				int n = (int) (bound - origin);
				Aggregator[] subAggregators = new Aggregator[n];
				for (int i = 0; i < n; i++) {
					subAggregators[i] = makeAggregator(dataSet, (Agg) distributionAggregate.getAggregateSupplier().get().buildAgg());
				}

				return (segment, row, result) -> {
					int index = (int) segment.rawData[column][row];
					subAggregators[index].process(segment, row, ((Object[])result)[index]);
				};
			}
		});
	}

	interface Aggregator {
		void process(Segment segment, int row, Object result);
	}
}
