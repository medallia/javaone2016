package com.medallia.dsl.interpreter;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;
import com.medallia.dsl.ConditionalExpression;
import com.medallia.dsl.Query;
import com.medallia.dsl.nodes.Expr;

public class QueryInterpreter<T> {

	private final Query<T> query;

	public QueryInterpreter(Query<T> query) {
		this.query = query;
	}

	public T eval(DataSet dataSet) {
		Expr expr = query.filters.stream()
				.reduce(ConditionalExpression::and)
				.orElseThrow(() -> new RuntimeException("empty expression")) // TODO: fix
				.buildTree();

		ExprInterpreter exprInterpreter = new ExprInterpreter(expr);

		for (Segment segment : dataSet.getSegments()) {
			final long[][] rawData = segment.rawData;
			final int nRows = rawData[0].length;
			for (int row = 0; row < nRows; row++) {
				if (exprInterpreter.eval(dataSet, segment, row)) {
					query.aggregateOp.update(dataSet, segment, row);;
				}
			}
		}

		return query.aggregateOp.getResult();
	}
}
