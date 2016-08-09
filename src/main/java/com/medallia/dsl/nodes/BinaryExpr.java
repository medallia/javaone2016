package com.medallia.dsl.nodes;

public abstract class BinaryExpr implements Expr {
	protected final Expr left;
	protected final Expr right;

	public BinaryExpr(Expr left, Expr right) {

		this.left = left;
		this.right = right;
	}
}
