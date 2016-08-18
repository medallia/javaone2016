package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.dsl.interpreter.QueryInterpreter;

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

		simple(dataSet);
		distribution(dataSet);
	}

	private static void distribution(DataSet dataSet) {
		Query<Map<Long,FieldStats>> query = newQuery()
				.filter(
						field("a").in(1, 2, 3)
								.or(field("b").is(3))
				)
				.aggregate(distributeOver("sex", () -> statsAggregate("ltr")));


		QueryInterpreter<Map<Long,FieldStats>> interpreter = new QueryInterpreter<>(query);
		System.out.println("Result: " + interpreter.eval(dataSet));
	}

	private static void simple(DataSet dataSet) throws IllegalAccessException, InstantiationException {
		Query<FieldStats> query = newQuery()
				.filter(
						field("a").in(1, 2, 3)
								.or(field("b").is(3))
				)
				.aggregate(statsAggregate("ltr"));

		final QueryInterpreter<FieldStats> interpreter = new QueryInterpreter<>(query);

		// TODO: use benchmark framework
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			FieldStats result = interpreter.eval(dataSet);
			long elapsed = System.nanoTime() - start;
			System.out.printf("Interpreter run %d, result: %s in (%.2f ms) %n", i, result, elapsed / 1e6);
		}

		final QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		Class<? extends CompiledQueryBase<FieldStats>> compiled = queryCompiler.compile();

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			CompiledQueryBase<FieldStats> compiledQuery = compiled.newInstance();
			dataSet.getSegments().forEach(compiledQuery::process);
			FieldStats result = compiledQuery.getResult();
			long elapsed = System.nanoTime() - start;
			System.out.printf("Compiled run %d, result: %s in (%.2f ms) %n", i, result, elapsed / 1e6);
		}
	}

}
