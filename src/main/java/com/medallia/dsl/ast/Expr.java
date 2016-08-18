package com.medallia.dsl.ast;

public interface Expr {
	<T> T visit(ExprVisitor<T> visitor);
}
