package com.medallia.benchmark;

import com.medallia.data.DataSet;
import com.medallia.data.FieldSpec;
import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
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

@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
public abstract class QueryBenchmark {
	protected final DataSet dataSet;

	protected final Supplier<CompiledQueryBase<FieldStats>> variantCompiledQuerySupplier;

	protected final Supplier<CompiledQueryBase<FieldStats>> compiledQuerySupplier;

	protected final QueryInterpreter<FieldStats> interpreter;

	protected final StreamQueryInterpreter<FieldStats> streamInterpreter;


	public QueryBenchmark(Query<FieldStats> query) {
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

		compiledQuerySupplier = new QueryCompiler<>(query, dataSet)
				.compile();
		variantCompiledQuerySupplier = new BranchReducingQueryCompiler<>(query, dataSet)
				.compile();
		interpreter = new QueryInterpreter<>(query);
		streamInterpreter = new StreamQueryInterpreter<>(query);

	}


	@Benchmark
	public void compiledQuery(Blackhole blackhole) {
		compiledQueryBenchmark(blackhole, compiledQuerySupplier);
	}

	@Benchmark
	public void variantCompiledQuery(Blackhole blackhole) {
		compiledQueryBenchmark(blackhole, this.variantCompiledQuerySupplier);
	}

	@Benchmark
	public void interpretedQuery(Blackhole blackhole) {
		blackhole.consume(interpreter.eval(dataSet));
	}

	@Benchmark
	public void streamInterpretedQuery(Blackhole blackhole) {
		blackhole.consume(streamInterpreter.eval(dataSet));
	}

	private void compiledQueryBenchmark(Blackhole blackhole, Supplier<CompiledQueryBase<FieldStats>> querySupplier) {
		CompiledQueryBase<FieldStats> compiledQuery = querySupplier.get();
		for (Segment segment : dataSet.getSegments()) {
			compiledQuery.process(segment);
		}
		blackhole.consume(compiledQuery.getResult());
	}

	public static void main(String[] args) throws RunnerException {
		Options opts = new OptionsBuilder()
				.include(".*" + SimpleQueryBenchmark.class.getSimpleName() + ".*")
				.include(".*" + ComplexQueryBenchmark.class.getSimpleName() + ".*")
				.forks(10)
				.build();
		new Runner(opts).run();
	}
}
