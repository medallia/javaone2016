package com.medallia.dsl.nodes;

import java.time.Period;

public class TimeInExpr implements Expr {
	private final String fieldName;
	private final Period period;

	public TimeInExpr(String fieldName, Period period) {

		this.fieldName = fieldName;
		this.period = period;
	}
}
