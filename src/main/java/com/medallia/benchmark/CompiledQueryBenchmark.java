package com.medallia.benchmark;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.dsl.compiler.QueryCompiler;

import java.util.function.Supplier;

public class CompiledQueryBenchmark
		extends QueryBenchmark<Supplier<CompiledQueryBase<FieldStats>>>
{
	public CompiledQueryBenchmark() {
		super(((dataSet, query) -> new QueryCompiler<>(query, dataSet).compile()), (dataSet, supplier) -> {
			CompiledQueryBase<FieldStats> compiledQuery = supplier.get();
			dataSet.getSegments().forEach(compiledQuery::process);
			return compiledQuery.getResult();
		});
	}
}
