package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.dsl.interpreter.QueryInterpreter;
import com.medallia.dsl.interpreter.StreamQueryInterpreter;
import org.junit.Test;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.constant;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.ConditionalExpression.not;
import static com.medallia.dsl.QueryBuilder.newQuery;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Simple cross check that all implementations yield the same value for the same query and data
 */
public class CrossCheckTest {
	@Test
	public void test() {
		DataSet dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("ltr", 0, 11)
		);

		Query<FieldStats> query = newQuery()
				.filter(
						field("a").in(1, 2, 3)
								.or(field("b").is(3)
								.and(not(constant(false))))
				)
				.filter(not(not(constant(true))))
				.aggregate(statsAggregate("ltr"));


		final QueryInterpreter<FieldStats> interpreter = new QueryInterpreter<>(query);
		FieldStats interpreterResult = interpreter.eval(dataSet);

		final QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		final CompiledQueryBase<FieldStats> compiledQuery = queryCompiler.compile().get();
		dataSet.getSegments().forEach(compiledQuery::process);
		FieldStats compiledResult = compiledQuery.getResult();

		final StreamQueryInterpreter<FieldStats> streamQueryInterpreter = new StreamQueryInterpreter<>(query);
		FieldStats streamResult = streamQueryInterpreter.eval(dataSet);

		final BranchReducingQueryCompiler<FieldStats> branchReducingQueryCompiler = new BranchReducingQueryCompiler<>(query, dataSet);
		final CompiledQueryBase<FieldStats> compiledQuery2 = branchReducingQueryCompiler.compile().get();
		dataSet.getSegments().forEach(compiledQuery2::process);
		FieldStats branchResult = compiledQuery2.getResult();

		System.out.println("            Interpreted result: " + interpreterResult);
		System.out.println("               Compiled result: " + compiledResult);
		System.out.println("     Stream Interpreted result: " + streamResult);
		System.out.println("Branch reduced compiled result: " + branchResult);

		assertThat(interpreterResult, is(compiledResult));
		assertThat(compiledResult, is(streamResult));
		assertThat(streamResult, is(branchResult));
	}
}
