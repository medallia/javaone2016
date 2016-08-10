package com.medallia.codegen;

/** Classloader that can be garbage collected after it's only class is unloaded */
public class DiscardableClassLoader {
	/**
	 * This method loads a single class in its own class loader.
	 */
	public static <T> Class<? extends T> classFromBytes(final Class<T> clazz, final String name, final byte[] b) {
		return new ClassLoader(DiscardableClassLoader.class.getClassLoader()) {
			Class<? extends T> c = defineClass(name, b, 0, b.length).asSubclass(clazz);
		}.c;
	}
}
