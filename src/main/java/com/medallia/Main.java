package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.interpreter.ExprInterpreter;

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
				new FieldSpec("ltr", 0, 11)
				);
		Query<FieldStats> query = newQuery()
				.filter(
						field("a").in(1, 2, 3)
								.or(field("b").is(3))
				)
				.aggregate(statsAggregate("ltr"));

		ExprInterpreter exprInterpreter = new ExprInterpreter(query.filters.get(0).buildTree());

		int count = 0;

		for (Segment segment : dataSet.getSegments()) {
			final long[][] rawData = segment.rawData;
			final int nRows = rawData[0].length;
			for (int row = 0; row < nRows; row++) {
				if (exprInterpreter.eval(dataSet, segment, row)) {
					++count;
				}
			}
		}

		System.out.println("count = " + count);
	}
}
