package com.medallia;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;
import com.medallia.dsl.interpreter.QueryInterpreter;
import com.medallia.dsl.interpreter.StreamQueryInterpreter;
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
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
public class QueryBenchmark {
	private final DataSet dataSet;

	private final Supplier<CompiledQueryBase<FieldStats>> querySupplier;

	private final QueryInterpreter<FieldStats> interpreter;

	private final StreamQueryInterpreter<FieldStats> streamInterpreter;


	public QueryBenchmark() {
		dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("sex", 0, 2),
				new FieldSpec("ltr", 0, 11)
		);

		final Query<FieldStats> query = newQuery()
			.filter(
				field("a").in(1, 2, 3).or(field("b").is(3))
			)
			.aggregate(statsAggregate("ltr"));

		QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		querySupplier = queryCompiler.compile();

		interpreter = new QueryInterpreter<>(query);
		streamInterpreter = new StreamQueryInterpreter<>(query);

	}

	@Benchmark
	public void compiledQuery() {
		CompiledQueryBase<FieldStats> compiledQuery = querySupplier.get();
		for (Segment segment : dataSet.getSegments()) {
			compiledQuery.process(segment);
		}
	}

	@Benchmark
	public void interpretedQuery() {
		interpreter.eval(dataSet);
	}

	@Benchmark
	public void streamInterpretedQuery() {
		streamInterpreter.eval(dataSet);
	}


	public static void main(String[] args) throws RunnerException {
		Options opts = new OptionsBuilder()
				.include(".*" + QueryBenchmark.class.getSimpleName() + ".*")
				.build();
		new Runner(opts).run();
	}
}
