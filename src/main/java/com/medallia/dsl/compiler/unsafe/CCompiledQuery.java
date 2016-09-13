package com.medallia.dsl.compiler.unsafe;

import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.unsafe.Driver;
import com.medallia.unsafe.Native;
import com.medallia.unsafe.NativeFunction;
import com.medallia.unsafe.NativeModule;
import com.medallia.unsafe.thunk.NativeBindings;
import com.medallia.unsafe.thunk.ThunkBuilder;

public class CCompiledQuery<T> {
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


	public CCompiledQuery(NativeModule implementation) {
		functions = BINDINGS.getFunctionPointers(implementation);
	}

	public native void process(Segment segment);

	public native T getResult();


	public static void main(String[] args) {
		final NativeModule implementation = Driver.compileInMemory("#include<jni.h>\n" +
				"void process(JNIEnv* env, jobject self, jobject segment) {  }\n" +
				"jobject getResult(JNIEnv* env, jobject self) { return NULL;}");

		CCompiledQuery<FieldStats> compiled = new CCompiledQuery<>(implementation);
		compiled.process(null);
		System.out.println("compiled.getResult() = " + compiled.getResult());;
	}
}
