package com.medallia.benchmark;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.ConditionalExpression.not;
import static com.medallia.dsl.QueryBuilder.newQuery;

@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
public abstract class QueryBenchmark<T> {

	private final StateInitializer<T> stateInitializer;
	private final QueryRunner<T> queryRunner;
	private final DataSet dataSet;

	private T state;

	protected QueryBenchmark(StateInitializer<T> stateInitializer, QueryRunner<T> queryRunner) {
		this.stateInitializer = stateInitializer;
		this.queryRunner = queryRunner;
		dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("c", 0, 11),
				new FieldSpec("d", 0, 5),
				new FieldSpec("e", 0, 20),
				new FieldSpec("f", 0, 5),
				new FieldSpec("ltr", 0, 11)
		);
	}

	@Setup(Level.Trial)
	public void setup() {
		state = null;
	}

	private void initializeState(Query<FieldStats> query) {
		if (state == null) {
			// this will happen only on the warmup iteration
			state = stateInitializer.initialize(dataSet, query);
		}
	}

	@Benchmark
	public void simpleQuery(Blackhole blackhole) {
		final Query<FieldStats> query = newQuery()
				.filter(field("a").in(1, 2, 3).or(field("b").is(3)))
				.aggregate(statsAggregate("ltr"));

		initializeState(query);

		blackhole.consume(queryRunner.run(dataSet, state));
	}

	@Benchmark
	public void complexQuery(Blackhole blackhole) {
		final Query<FieldStats> query = newQuery()
				.filter(field("a").in(1, 2, 3, 5).or(field("b").is(3)))
				.filter(field("c").in(3, 5, 8, 1, 2).or(field("d").in(1,3,4)))
				.filter(not(field("e").in(2, 5, 8,10,12,13,14,15,17)).or(field("f").in(1,4,2)))
				.aggregate(statsAggregate("ltr"));

		initializeState(query);

		blackhole.consume(queryRunner.run(dataSet, state));
	}

	public interface StateInitializer<T> {
		T initialize(DataSet dataSet, Query<FieldStats> query);
	}
	public interface QueryRunner<T> {
		FieldStats run(DataSet dataSet, T state);
	}

	public static void main(String[] args) throws RunnerException {
		Options opts = new OptionsBuilder()
				.include(CompiledQueryBenchmark.class.getSimpleName())
				.include(VariantCompiledQueryBenchmark.class.getSimpleName())
				.include(InterpretedQueryBenchmark.class.getSimpleName())
				.include(StreamInterpretedQueryBenchmark.class.getSimpleName())
				.forks(1)
				.build();
		new Runner(opts).run();
	}
}
