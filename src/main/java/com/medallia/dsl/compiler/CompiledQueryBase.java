package com.medallia.dsl.compiler;

import com.medallia.data.Segment;

public abstract class CompiledQueryBase<T> {
	protected T result;

	public abstract void process(Segment segment);

	public T getResult() {
		return result;
	}
}
