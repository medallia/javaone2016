package com.medallia.dsl;

import java.util.Objects;

public final class FieldStats {
	public long count;
	public double sum;

	@Override
	public String toString() {
		return String.format("Stats[count: %d, sum: %s, avg: %s]", count, sum, sum/count);
	}

	@Override
	public int hashCode() {
		return Objects.hash(count, sum);
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null
				&& obj.getClass() == getClass()
				&& ((FieldStats)obj).count == count
				&& ((FieldStats)obj).sum   == sum;
	}
}
