package com.medallia.data;

public class FieldSpec {
	private final String name;
	private final long maxValue;
	private final long minValue;

	public FieldSpec(String name, long minValue, long maxValue) {
		this.name = name;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public String getName() {
		return name;
	}

	public long getMaxValue() {
		return maxValue;
	}

	public long getMinValue() {
		return minValue;
	}
}
