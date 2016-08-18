package com.medallia.dsl;

import com.medallia.dsl.ast.Agg;
import com.medallia.dsl.ast.DistributionAggregate;
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

	public static <T> Aggregate<T[]> distributeOver(String fieldName, Supplier<Aggregate<T>> aggregateSupplier) {
		return new Aggregate<>(() -> new DistributionAggregate<>(fieldName, aggregateSupplier));
	}

	public Agg<R> buildAgg() {
		return nodeBuilder.get();
	}
}