package com.medallia.dsl.nodes;

public class NotExpr extends UnaryExpr {
	public NotExpr(Expr target) {
		super(target);
	}

	@Override
	public void visit(ExprVisitor visitor) { visitor.visit(this); }
}
