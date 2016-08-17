package com.medallia.dsl.nodes;

public class NotExpr extends UnaryExpr {
	public NotExpr(Expr target) {
		super(target);
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) { return visitor.visit(this); }
}
