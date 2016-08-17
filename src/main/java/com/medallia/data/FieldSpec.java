package com.medallia.data;

public class FieldSpec {
	private final String name;
	private final long maxValue;
	private final long minValue;

	public FieldSpec(String name, long maxValue, long minValue) {
		this.name = name;
		this.maxValue = maxValue;
		this.minValue = minValue;
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
