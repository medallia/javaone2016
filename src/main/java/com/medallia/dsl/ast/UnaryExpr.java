package com.medallia.dsl.ast;

public abstract class UnaryExpr implements Expr {
	protected final Expr target;

	public UnaryExpr(Expr target) {
		this.target = target;
	}

	public Expr getTarget() {
		return target;
	}
}
