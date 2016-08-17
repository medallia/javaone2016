package com.medallia.dsl.nodes;

public class OrExpr extends BinaryExpr {
	public OrExpr(Expr left, Expr right) {
		super(left, right);
	}

	@Override
	public void visit(ExprVisitor visitor) { visitor.visit(this); }
}
