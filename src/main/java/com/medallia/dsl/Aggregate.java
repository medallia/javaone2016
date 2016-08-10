package com.medallia.dsl;

public interface Aggregate<R> {

	static Aggregate<FieldStats> statsAggregate(String fieldName) {
		return new Aggregate<FieldStats>() {};
	}
}