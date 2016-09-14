package com.medallia.benchmark;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.unsafe.CCompiledQuery;
import com.medallia.dsl.compiler.unsafe.CQueryCompiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.function.Supplier;

public class UnsafeCompiledQueryBenchmark
		extends QueryBenchmark<Supplier<CompiledQueryBase<FieldStats>>>
{
	public UnsafeCompiledQueryBenchmark() {
		super(((dataSet, query) -> new CQueryCompiler<>(query, dataSet).compile()), (dataSet, supplier) -> {
			CompiledQueryBase<FieldStats> compiledQuery = supplier.get();
			dataSet.getSegments().forEach(compiledQuery::process);
			return compiledQuery.getResult();
		});
	}

	public static void main(String[] args) throws RunnerException {
		Options opts = new OptionsBuilder()
				.include(UnsafeCompiledQueryBenchmark.class.getSimpleName())
				.forks(2)
				.build();
		new Runner(opts).run();
	}
}
