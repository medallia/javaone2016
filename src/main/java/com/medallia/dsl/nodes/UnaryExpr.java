package com.medallia.dsl.nodes;

public abstract class UnaryExpr implements Expr {
	protected final Expr target;

	public UnaryExpr(Expr target) {
		this.target = target;
	}
}
