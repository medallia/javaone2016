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
import org.openjdk.jmh.infra.Blackhole;
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
public abstract class QueryBenchmark {
	protected final DataSet dataSet;

	protected final Supplier<CompiledQueryBase<FieldStats>> querySupplier;

	protected final QueryInterpreter<FieldStats> interpreter;

	protected final StreamQueryInterpreter<FieldStats> streamInterpreter;


	public QueryBenchmark(Query<FieldStats> query) {
		dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("c", 0, 5),
				new FieldSpec("d", 0, 5),
				new FieldSpec("e", 0, 5),
				new FieldSpec("f", 0, 5),
				new FieldSpec("ltr", 0, 11)
		);

		QueryCompiler<FieldStats> queryCompiler = new QueryCompiler<>(query, dataSet);
		querySupplier = queryCompiler.compile();

		interpreter = new QueryInterpreter<>(query);
		streamInterpreter = new StreamQueryInterpreter<>(query);

	}

	@Benchmark
	public void compiledQuery(Blackhole blackhole) {
		CompiledQueryBase<FieldStats> compiledQuery = querySupplier.get();
		for (Segment segment : dataSet.getSegments()) {
			compiledQuery.process(segment);
		}
		blackhole.consume(compiledQuery.getResult());
	}

	@Benchmark
	public void interpretedQuery(Blackhole blackhole) {
		blackhole.consume(interpreter.eval(dataSet));
	}

	@Benchmark
	public void streamInterpretedQuery(Blackhole blackhole) {
		blackhole.consume(streamInterpreter.eval(dataSet));
	}

	public static class ComplexQueryBenchmark extends QueryBenchmark {
		public ComplexQueryBenchmark() {
			super(newQuery()
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
					.aggregate(statsAggregate("ltr")));
		}
	}

	public static class SimpleQueryBenchmark extends QueryBenchmark {
		public SimpleQueryBenchmark() {
			super(newQuery()
					.filter(field("a").in(1, 2, 3))
					.aggregate(statsAggregate("ltr")));
		}
	}

	public static void main(String[] args) throws RunnerException {

		Options opts = new OptionsBuilder()
				.include(".*" + QueryBenchmark.class.getSimpleName() + ".*")
				.build();
		new Runner(opts).run();
	}
}
