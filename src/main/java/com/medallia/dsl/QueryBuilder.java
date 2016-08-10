package com.medallia.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryBuilder {

	private final List<ConditionalExpression> filters = new ArrayList<>();

	public static QueryBuilder newQuery() {
		return new QueryBuilder();
	}

	public QueryBuilder filter(ConditionalExpression expression) {
		filters.add(expression);
		return this;
	}

	public <R> Query<R> aggregate(Aggregate<R> aggregateOp) {
		return new Query<R>(Collections.unmodifiableList(filters), aggregateOp);
	}
}
