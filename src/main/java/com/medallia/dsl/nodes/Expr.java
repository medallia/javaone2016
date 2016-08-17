package com.medallia.dsl.nodes;

public interface Expr {
	<T> T visit(ExprVisitor<T> visitor);
}
