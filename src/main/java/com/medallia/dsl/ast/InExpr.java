package com.medallia.dsl.ast;

public class InExpr implements Expr {
	private final String fieldName;
	private final int[] values;

	public <T> InExpr(String fieldName, int[] values) {
		this.fieldName = fieldName;
		this.values = values;
	}

	public String getFieldName() {
		return fieldName;
	}

	public int[] getValues() {
		return values;
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) { return visitor.visit(this); }

}
