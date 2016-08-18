package com.medallia.dsl.interpreter;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.ast.Agg;
import com.medallia.dsl.ast.AggVisitor;
import com.medallia.dsl.ast.AndExpr;
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
		final Aggregator aggregator = makeAggregator(dataSet, agg, result);

		for (Segment segment : dataSet.getSegments()) {
			final long[][] rawData = segment.rawData;
			final int nRows = rawData[0].length;
			for (int row = 0; row < nRows; row++) {
				if (eval(expr, dataSet, segment, row)) {
					aggregator.process(segment, row);
				}
			}
		}

		return result;
	}

	private Aggregator makeAggregator(DataSet dataSet, Agg<?> agg, Object result) {
		return agg.visit(new AggVisitor<Aggregator>() {
			@Override
			public Aggregator visit(StatsAggregate statsAggregate) {
				int column = dataSet.getFieldByName(statsAggregate.getFieldName())
						.getColumn();
				return (segment, row) -> {
					((FieldStats)result).count++;
					((FieldStats)result).sum += segment.rawData[column][row];
				};
			}

			@Override
			public Aggregator visit(DistributionAggregate<?> distributionAggregate) {
				// Build an interpreter array
				Object[] r = (Object[]) result; // this should hold
				Aggregator[] subAggregators = new Aggregator[r.length];
				for (int i = 0; i < r.length; i++) {
					subAggregators[i] = makeAggregator(dataSet, (Agg) distributionAggregate.getAggregateSupplier().get().buildAgg(), r[i]);
				}

				// Capture distribution field column
				int column = dataSet.getFieldByName(distributionAggregate.getFieldName()).getColumn();
				return (segment, row) -> {
					subAggregators[(int) segment.rawData[column][row]].process(segment, row);
				};
			}
		});
	}


	private boolean eval(Expr expression, DataSet dataSet, Segment segment, int row) {
		return expression.visit(new ExprVisitor<Boolean>() {
			@Override
			public Boolean visit(AndExpr andExpr) {
				return andExpr.getLeft().visit(this) && andExpr.getRight().visit(this);
			}

			@Override
			public Boolean visit(InExpr inExpr) {
				return LongStream.of(inExpr.getValues())
						.filter(v -> v == segment.rawData[dataSet.getFieldByName(inExpr.getFieldName()).getColumn()][row])
						.findAny()
						.isPresent();
			}

			@Override
			public Boolean visit(NotExpr notExpr) {
				return !notExpr.getTarget().visit(this);
			}

			@Override
			public Boolean visit(OrExpr orExpr) {
				return orExpr.getLeft().visit(this) || orExpr.getRight().visit(this);
			}
		});
	}

	interface Aggregator {
		void process(Segment segment, int row);

	}
}
