package com.medallia.dsl;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface Aggregate<R> {
	void update(DataSet dataSet, Segment segment, int row);
	R getResult();

	static Aggregate<FieldStats> statsAggregate(String fieldName) {
		return new StatsAggregate(fieldName);
	}

	static <T> Aggregate<Map<Long, T>> distributeOver(String fieldName, Supplier<Aggregate<T>> aggregateSupplier) {
		return new DistributionAggregate<>(fieldName, aggregateSupplier);
	}

	class StatsAggregate implements Aggregate<FieldStats> {
		private final FieldStats stats = new FieldStats();
		private final String fieldName;
		private int fieldColumn = -1;
		public StatsAggregate(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public FieldStats getResult() {
			return stats;
		}

		@Override
		public void update(DataSet dataSet, Segment segment, int row) {
			if (fieldColumn == -1) {
				fieldColumn = dataSet.getFieldByName(fieldName).getColumn();
			}
			stats.sum += segment.rawData[fieldColumn][row];
			stats.count++;
		}
	}

	class DistributionAggregate<T> implements Aggregate<Map<Long, T>> {
		private final String fieldName;
		private final Supplier<Aggregate<T>> aggregateSupplier;
		private int fieldColumn = -1;
		private Map<Long, Aggregate<T>> partialResults;

		public DistributionAggregate(String fieldName, Supplier<Aggregate<T>> aggregateSupplier) {
			this.fieldName = fieldName;
			this.aggregateSupplier = aggregateSupplier;
			this.partialResults = new HashMap<>();
		}

		@Override
		public void update(DataSet dataSet, Segment segment, int row) {
			if (fieldColumn == -1) {
				fieldColumn = dataSet.getFieldByName(fieldName).getColumn();
			}
			partialResults.computeIfAbsent(segment.rawData[fieldColumn][row], k -> aggregateSupplier.get())
					.update(dataSet, segment, row);
		}

		@Override
		public Map<Long, T> getResult() {
			return partialResults
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getResult()));
		}
	}
}