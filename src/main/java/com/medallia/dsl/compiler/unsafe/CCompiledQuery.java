package com.medallia.dsl.compiler.unsafe;

import com.medallia.data.Segment;
import com.medallia.dsl.compiler.CompiledQueryBase;
import com.medallia.unsafe.Native;
import com.medallia.unsafe.NativeModule;
import com.medallia.unsafe.thunk.NativeBindings;
import com.medallia.unsafe.thunk.ThunkBuilder;

public class CCompiledQuery<T> extends CompiledQueryBase<T> {
	/**
	 * Create native bindings for this class.
	 * Should be kept around for the lifetime of this class.
	 */
	private static final NativeBindings BINDINGS = ThunkBuilder.initializeNative(CCompiledQuery.class);

	/**
	 * Holds pointers to functions needed by the native code.
	 * Must be initialized by a call to {@link com.medallia.unsafe.thunk.NativeBindings#getFunctionPointers(com.medallia.unsafe.NativeModule)}
	 * passing a suitable
	 */
	@Native
	private final long[] functions;

	public CCompiledQuery(NativeModule implementation, T result) {
		functions = BINDINGS.getFunctionPointers(implementation);
		this.result = result;
	}

	public native void init();

	public native void process(Segment segment);
}
