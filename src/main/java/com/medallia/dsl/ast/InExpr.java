package com.medallia.dsl.ast;

public class InExpr implements Expr {
	private final String fieldName;
	private final long[] values;

	public <T> InExpr(String fieldName, long[] values) {
		this.fieldName = fieldName;
		this.values = values;
	}

	public String getFieldName() {
		return fieldName;
	}

	public long[] getValues() {
		return values;
	}

	@Override
	public <T> T visit(ExprVisitor<T> visitor) { return visitor.visit(this); }

}
