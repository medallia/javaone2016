package com.medallia.codegen;

/** Classloader that can be garbage collected after it's only class is unloaded */
public class DiscardableClassLoader {
	/**
	 * This method loads a single class in its own class loader.
	 * The classloader will be a child of the baseClass' classloader.
	 */
	public static <T> Class<? extends T> classFromBytes(final Class<T> baseClass, final String name, final byte[] bytecode) {
		return new ClassLoader(baseClass.getClassLoader()) {
			Class<? extends T> c = defineClass(name, bytecode, 0, bytecode.length).asSubclass(baseClass);
		}.c;
	}
}
