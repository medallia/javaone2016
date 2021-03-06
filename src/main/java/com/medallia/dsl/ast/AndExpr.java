package com.medallia.dsl.ast;

public class AndExpr extends BinaryExpr {
	public AndExpr(Expr left, Expr right) {
		super(left, right);
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) { return visitor.visit(this); }
}
