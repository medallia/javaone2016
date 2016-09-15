package com.medallia.data;

public class FieldSpec {
	private final String name;
	private final int origin;
	private final int bound;

	public FieldSpec(String name, int origin, int bound) {
		this.name = name;
		this.origin = origin;
		this.bound = bound;
	}

	public String getName() {
		return name;
	}

	public int getBound() {
		return bound;
	}

	public int getOrigin() {
		return origin;
	}
}
