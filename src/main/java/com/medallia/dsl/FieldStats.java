package com.medallia.dsl;

public class FieldStats {
	public long count;
	public double sum;

	@Override
	public String toString() {
		return String.format("Stats[count: %d, sum: %s, avg: %s]", count, sum, sum/count);
	}
}
