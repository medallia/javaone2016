package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.dsl.interpreter.QueryInterpreter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

@State(Scope.Benchmark)
public class CompiledQueryBenchmark {

	private final DataSet dataSet;

	private final Query<FieldStats> query;

	private final Supplier<CompiledQueryBase<FieldStats>> querySupplier;


	public CompiledQueryBenchmark() {
		dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("sex", 0, 2),
				new FieldSpec("ltr", 0, 11)
		);

		query = newQuery()
			.filter(
				field("a").in(1, 2, 3).or(field("b").is(3))
			)
			.aggregate(statsAggregate("ltr"));

		QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		querySupplier = queryCompiler.compile();
	}

	@Benchmark
	@Warmup(iterations = 2)
	@Measurement(iterations = 5)
	@OutputTimeUnit(TimeUnit.SECONDS)
	public void simpleQuery() {
		CompiledQueryBase<FieldStats> compiledQuery = querySupplier.get();
		dataSet.getSegments().forEach(compiledQuery::process);
	}

	public static void main(String[] args) throws RunnerException {
		Options opts = new OptionsBuilder().include(".*CompiledQueryBenchmark.*").build();
		new Runner(opts).run();
	}
}
