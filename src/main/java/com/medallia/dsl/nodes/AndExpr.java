package com.medallia.dsl.nodes;

public class AndExpr extends BinaryExpr {
	public AndExpr(Expr left, Expr right) {
		super(left, right);
	}

	@Override
	public void visit(ExprVisitor visitor) { visitor.visit(this); }
}
