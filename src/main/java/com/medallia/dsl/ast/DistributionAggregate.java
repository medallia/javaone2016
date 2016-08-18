package com.medallia.dsl.ast;

import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.Aggregate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DistributionAggregate<T> implements Agg<T[]> {
	private final String fieldName;

	private final Supplier<Aggregate<T>> aggregateSupplier;

	public DistributionAggregate(String fieldName, Supplier<Aggregate<T>> aggregateSupplier) {
		this.fieldName = fieldName;
		this.aggregateSupplier = aggregateSupplier;
	}

	@Override
	public T[] makeResult(DataSet dataSet) {
		final FieldDefinition fieldDef = dataSet.getFieldByName(fieldName);
		final FieldSpec fieldSpec = fieldDef.getFieldSpec();
		final List<T> result = new ArrayList<T>();
		for (long i = fieldSpec.getOrigin(); i < fieldSpec.getBound(); i++) {
			result.add(aggregateSupplier.get().buildAgg().makeResult(dataSet));
		}

		T[] resultArray = (T[]) Array.newInstance(result.get(0).getClass(), (int) (fieldSpec.getBound() - fieldSpec.getOrigin()));
		return result.toArray(resultArray);
	}

	public String getFieldName() {
		return fieldName;
	}

	public Supplier<Aggregate<T>> getAggregateSupplier() {
		return aggregateSupplier;
	}

	@Override
	public <R> R visit(AggVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
