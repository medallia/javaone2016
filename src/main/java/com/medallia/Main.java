package com.medallia;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class Main {
	public static void main(String[] args) throws IllegalAccessException, InstantiationException {
		Query<FieldStats> query = newQuery()
				.filter(
						field("a", String.class).in("A", "B", "C")
								.or(field("b", Integer.class).is(3))
				)
				.aggregate(statsAggregate("ltr"));
	}
}
