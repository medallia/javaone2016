package com.medallia.dsl.ast;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;
import com.medallia.dsl.Aggregate;
import com.medallia.dsl.FieldStats;

public class StatsAggregate implements Agg<FieldStats> {
	private final String fieldName;
	public StatsAggregate(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public FieldStats makeResult(DataSet dataSet) {
		return new FieldStats();
	}

	@Override
	public <T> T visit(AggVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
