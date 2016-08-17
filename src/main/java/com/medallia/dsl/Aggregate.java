package com.medallia.dsl;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;

public interface Aggregate<R> {
	void update(DataSet dataSet, Segment segment, int row);
	R getResult();

	static Aggregate<FieldStats> statsAggregate(String fieldName) {
		return new StatsAggregate(fieldName);
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
}