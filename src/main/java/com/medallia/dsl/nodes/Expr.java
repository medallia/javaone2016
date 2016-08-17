package com.medallia.dsl.nodes;

public interface Expr {
	void visit(ExprVisitor visitor);
}
