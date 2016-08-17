package com.medallia.dsl.nodes;

public class OrExpr extends BinaryExpr {
	public OrExpr(Expr left, Expr right) {
		super(left, right);
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) { return visitor.visit(this); }
}
