package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.dsl.compiler.unsafe.CCompiledQuery;
import com.medallia.dsl.compiler.unsafe.CQueryCompiler;
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


		FieldStats interpreterResult = useQueryInterpreter(dataSet, query);

		FieldStats compiledResult = useSimpleCompiler(dataSet, query);

		FieldStats streamResult = useStreamInterpreter(dataSet, query);

		FieldStats branchResult = useBranchReducingCompiler(dataSet, query);

		FieldStats cResult = useCCompiler(dataSet, query);

		System.out.println("            Interpreted result: " + interpreterResult);
		System.out.println("               Compiled result: " + compiledResult);
		System.out.println("     Stream Interpreted result: " + streamResult);
		System.out.println("Branch reduced compiled result: " + branchResult);
		System.out.println("             C Compiler result: " + cResult);

		assertThat(interpreterResult, is(compiledResult));
		assertThat(compiledResult, is(streamResult));
		assertThat(streamResult, is(branchResult));
		if (cResult != null) {
			assertThat(branchResult, is(cResult));
		}
	}

	private FieldStats useQueryInterpreter(DataSet dataSet, Query<FieldStats> query) {
		final QueryInterpreter<FieldStats> interpreter = new QueryInterpreter<>(query);
		return interpreter.eval(dataSet);
	}

	private FieldStats useSimpleCompiler(DataSet dataSet, Query<FieldStats> query) {
		final QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		final CompiledQueryBase<FieldStats> compiledQuery = queryCompiler.compile().get();
		dataSet.getSegments().forEach(compiledQuery::process);
		return compiledQuery.getResult();
	}

	private FieldStats useStreamInterpreter(DataSet dataSet, Query<FieldStats> query) {
		final StreamQueryInterpreter<FieldStats> streamQueryInterpreter = new StreamQueryInterpreter<>(query);
		return streamQueryInterpreter.eval(dataSet);
	}

	private FieldStats useBranchReducingCompiler(DataSet dataSet, Query<FieldStats> query) {
		final BranchReducingQueryCompiler<FieldStats> branchReducingQueryCompiler = new BranchReducingQueryCompiler<>(query, dataSet);
		final CompiledQueryBase<FieldStats> compiledQuery2 = branchReducingQueryCompiler.compile().get();
		dataSet.getSegments().forEach(compiledQuery2::process);
		return compiledQuery2.getResult();
	}

	private FieldStats useCCompiler(DataSet dataSet, Query<FieldStats> query) {
		try {
			final CQueryCompiler<FieldStats> cQueryCompiler = new CQueryCompiler<>(query, dataSet);
			final CompiledQueryBase<FieldStats> compiledQuery3 = cQueryCompiler.compile().get();
			dataSet.getSegments().forEach(compiledQuery3::process);
			return compiledQuery3.getResult();
		} catch (UnsatisfiedLinkError e) {
			// unsafe library not present (most likely), bypass this test
			return null;
		}
	}
}
