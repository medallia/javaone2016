package com.medallia.dsl.nodes;

public class InExpr implements Expr {
	private final String fieldName;
	private final Class<?> fieldType;
	private final Object[] values;

	public <T> InExpr(String fieldName, Class<T> fieldType, T[] values) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.values = values;
	}
}
