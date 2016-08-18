package com.medallia.dsl.ast;

import com.medallia.data.DataSet;

public interface Agg<R> {

	R makeResult(DataSet dataSet);

	<T> T visit(AggVisitor<T> visitor);
}
