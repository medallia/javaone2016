package com.medallia.data;

public class FieldSpec {
	private final String name;
	private final long origin;
	private final long bound;

	public FieldSpec(String name, long origin, long bound) {
		this.name = name;
		this.origin = origin;
		this.bound = bound;
	}

	public String getName() {
		return name;
	}

	public long getBound() {
		return bound;
	}

	public long getOrigin() {
		return origin;
	}
}
