package com.medallia.dsl;

import com.medallia.dsl.ast.Expr;

import java.util.List;

public class Query<R> {
	public final List<ConditionalExpression> filters;
	public final Aggregate<R> aggregateOp;

	public Query(List<ConditionalExpression> filters, Aggregate<R> aggregateOp) {
		this.filters = filters;
		this.aggregateOp = aggregateOp;
	}

	public Expr buildExpressionTree() {
		return filters.stream()
				.reduce(ConditionalExpression::and)
				.orElseGet(() -> ConditionalExpression.constant(true))
				.buildTree();
	}
}
