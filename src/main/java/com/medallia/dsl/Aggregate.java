package com.medallia.dsl;

import com.medallia.dsl.ast.Agg;
import com.medallia.dsl.ast.StatsAggregate;

import java.util.function.Supplier;

public class Aggregate<R> {
	private final Supplier<Agg<R>> nodeBuilder;

	private Aggregate(Supplier<Agg<R>> nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public static Aggregate<FieldStats> statsAggregate(String fieldName) {
		return new Aggregate<>(() -> new StatsAggregate(fieldName));
	}

	public Agg<R> buildAgg() {
		return nodeBuilder.get();
	}
}