package com.medallia.benchmark;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class SimpleQueryBenchmark extends QueryBenchmark {
	public SimpleQueryBenchmark() {
		super(newQuery()
				.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
				.aggregate(statsAggregate("ltr")));
	}
}
