package com.medallia.dsl;

import java.util.List;

public class Query<R> {
	public final List<ConditionalExpression> filters;
	public final Aggregate<R> aggregateOp;

	public Query(List<ConditionalExpression> filters, Aggregate<R> aggregateOp) {
		this.filters = filters;
		this.aggregateOp = aggregateOp;
	}
}
