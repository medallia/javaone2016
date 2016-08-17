package com.medallia.dsl.nodes;

import java.time.LocalDate;

public class TimeIsExpr implements Expr {
	private final String fieldName;
	private final LocalDate value;

	public TimeIsExpr(String fieldName, LocalDate value) {
		this.fieldName = fieldName;
		this.value = value;
	}

	@Override
	public void visit(ExprVisitor visitor) { visitor.visit(this); }
}
