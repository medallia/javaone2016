package com.medallia.benchmark;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.compiler.BranchReducingQueryCompiler;
import com.medallia.dsl.compiler.CompiledQueryBase;

import java.util.function.Supplier;

public class VariantCompiledQueryBenchmark
		extends QueryBenchmark<Supplier<CompiledQueryBase<FieldStats>>>
{
	public VariantCompiledQueryBenchmark() {
		super(((dataSet, query) -> new BranchReducingQueryCompiler<>(query, dataSet).compile()), (dataSet, supplier) -> {
			CompiledQueryBase<FieldStats> compiledQuery = supplier.get();
			dataSet.getSegments().forEach(compiledQuery::process);
			return compiledQuery.getResult();
		});
	}
}
