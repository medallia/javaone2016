package com.medallia.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class QueryBuilder {

	private final List<ConditionalExpression> filters = new ArrayList<>();

	public static QueryBuilder newQuery() {
		return new QueryBuilder();
	}

	QueryBuilder filter(ConditionalExpression expression) {
		filters.add(expression);
		return this;
	}


	<R> Future<R> aggregate(Aggregate<R> aggregateOp) {
		throw new UnsupportedOperationException();
	}
}
