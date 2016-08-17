package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.data.Segment;
import com.medallia.dsl.ConditionalExpression;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.interpreter.ExprInterpreter;
import com.medallia.dsl.interpreter.QueryInterpreter;
import com.medallia.dsl.nodes.Expr;

import java.util.Map;

import static com.medallia.dsl.Aggregate.distributeOver;
import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class Main {
	public static void main(String[] args) throws IllegalAccessException, InstantiationException {
		DataSet dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				20_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("sex", 0, 2),
				new FieldSpec("ltr", 0, 11)
				);

		Query<Map<Long,FieldStats>> query = newQuery()
				.filter(
						field("a").in(1, 2, 3)
								.or(field("b").is(3))
				)
				.aggregate(distributeOver("sex", () -> statsAggregate("ltr")));


		QueryInterpreter<Map<Long,FieldStats>> interpreter = new QueryInterpreter<>(query);
		System.out.println("Result: " + interpreter.eval(dataSet));
	}
}
