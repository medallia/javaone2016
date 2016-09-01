package com.medallia.benchmark;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.interpreter.QueryInterpreter;

public class InterpretedQueryBenchmark
		extends QueryBenchmark<QueryInterpreter<FieldStats>>
{
	public InterpretedQueryBenchmark() {
		super(((dataSet, query) -> new QueryInterpreter<>(query)),
				(dataSet, interpreter) -> interpreter.eval(dataSet));
	}
}
