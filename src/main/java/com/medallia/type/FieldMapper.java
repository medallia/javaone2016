package com.medallia.type;

import java.util.function.Function;

public interface FieldMapper<T> {
	T fromStorage(long val);
	long toStorage(T val);

	static <T> FieldMapper<T> make(Function<T,Long> toStorage, Function<Long, T> from) {
		return new FieldMapper<T>() {
			@Override
			public T fromStorage(long val) {
				return from.apply(val);
			}

			@Override
			public long toStorage(T val) {
				return toStorage.apply(val);
			}
		};
	}
}
