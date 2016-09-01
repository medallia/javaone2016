package com.medallia.benchmark;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.ConditionalExpression.not;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class ComplexQueryBenchmark extends QueryBenchmark {
	public ComplexQueryBenchmark() {
		super(newQuery()
				.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
				.filter(field("c").in(3, 5, 8).or(field("d").in(1,3,4)))
				.filter(not(field("e").in(2, 5, 8,10,12,13,14,15,17)).or(field("f").in(1,4,2)))
				.aggregate(statsAggregate("ltr")));
	}
}
