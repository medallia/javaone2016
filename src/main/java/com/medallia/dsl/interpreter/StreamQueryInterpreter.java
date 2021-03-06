package com.medallia.dsl.interpreter;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;
import com.medallia.dsl.Query;
import com.medallia.dsl.ast.Agg;
import com.medallia.dsl.ast.AndExpr;
import com.medallia.dsl.ast.ConstantExpr;
import com.medallia.dsl.ast.Expr;
import com.medallia.dsl.ast.ExprVisitor;
import com.medallia.dsl.ast.InExpr;
import com.medallia.dsl.ast.NotExpr;
import com.medallia.dsl.ast.OrExpr;
import com.medallia.dsl.interpreter.QueryInterpreter.Aggregator;

import java.util.Arrays;

public class StreamQueryInterpreter<T> {

	private final Query<T> query;

	public StreamQueryInterpreter(Query<T> query) {
		this.query = query;
	}

	public T eval(DataSet dataSet) {
		final Expr expr = query.buildExpressionTree();
		final Agg<T> agg = query.aggregateOp.buildAgg();
		final Aggregator aggregator = QueryInterpreter.makeAggregator(dataSet, agg);
		final Filter filter = makeFilter(expr, dataSet);

		final T result = agg.makeResult(dataSet);
		for (Segment segment : dataSet.getSegments()) {
			final int nRows = segment.rawData[0].length;
			for (int row = 0; row < nRows; row++) {
				if (filter.eval(segment, row)) {
					aggregator.process(segment, row, result);
				}
			}
		}
		return result;
	}

	private Filter makeFilter(Expr expression, DataSet dataSet) {
		return expression.visit(new ExprVisitor<Filter>() {
			@Override
			public Filter visit(AndExpr andExpr) {
				Filter left = andExpr.getLeft().visit(this);
				Filter right = andExpr.getRight().visit(this);
				return (segment, row) -> left.eval(segment, row) && right.eval(segment, row);
			}

			@Override
			public Filter visit(InExpr inExpr) {
				final int[] sortedValues = inExpr.getValues().clone();
				Arrays.sort(sortedValues);
				int column = dataSet.getFieldByName(inExpr.getFieldName()).getColumn();
				if (sortedValues.length > 10) {
					return (segment, row) -> Arrays.binarySearch(sortedValues, segment.rawData[column][row]) >= 0;
				} else {
					return (segment, row) -> {
						int val = segment.rawData[column][row];
						for (int sortedValue : sortedValues) {
							if (sortedValue == val) return true;
						}
						return false;
					};
				}
			}

			@Override
			public Filter visit(NotExpr notExpr) {
				Filter target = notExpr.getTarget().visit(this);
				return (segment, row) -> !target.eval(segment, row);
			}

			@Override
			public Filter visit(OrExpr orExpr) {
				Filter left = orExpr.getLeft().visit(this);
				Filter right = orExpr.getRight().visit(this);
				return (segment, row) -> left.eval(segment, row) || right.eval(segment, row);
			}

			@Override
			public Filter visit(ConstantExpr constantExpr) {
				boolean val = constantExpr.value;
				return (segment, row) -> val;
			}
		});
	}

	@FunctionalInterface
	interface Filter {
		boolean eval(Segment segment, int row);
	}
}
