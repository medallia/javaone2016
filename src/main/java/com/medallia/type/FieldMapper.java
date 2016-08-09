package com.medallia.type;

import java.util.function.Function;

public interface FieldMapper<T> {
	T fromStorage(int val);
	int toStorage(T val);

	static <T> FieldMapper<T> make(Function<T,Integer> toStorage, Function<Integer, T> from) {
		return new FieldMapper<T>() {
			@Override
			public T fromStorage(int val) {
				return from.apply(val);
			}

			@Override
			public int toStorage(T val) {
				return toStorage.apply(val);
			}
		};
	}
}
