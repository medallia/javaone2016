package com.medallia.benchmark;

import com.medallia.dsl.FieldStats;
import com.medallia.dsl.interpreter.StreamQueryInterpreter;

public class StreamInterpretedQueryBenchmark
		extends QueryBenchmark<StreamQueryInterpreter<FieldStats>>
{
	public StreamInterpretedQueryBenchmark() {
		super(((dataSet, query) -> new StreamQueryInterpreter<>(query)),
				(dataSet, interpreter) -> interpreter.eval(dataSet));
	}
}
