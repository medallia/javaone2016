package com.medallia.dsl.ast;

public class ConstantExpr implements Expr {
	public final boolean value;

	public ConstantExpr(boolean value) {
		this.value = value;
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
