package com.medallia.dsl.compiler.unsafe;

import com.medallia.data.Segment;
import com.medallia.dsl.FieldStats;
import com.medallia.unsafe.Driver;
import com.medallia.unsafe.Native;
import com.medallia.unsafe.NativeModule;
import com.medallia.unsafe.thunk.NativeBindings;
import com.medallia.unsafe.thunk.ThunkBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class CCompiledQuery {
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

	@Native
	private FieldStats result = new FieldStats();


	public CCompiledQuery(NativeModule implementation) {
		functions = BINDINGS.getFunctionPointers(implementation);
	}

	public native void init();

	public native void process(Segment segment);

	public FieldStats getResult() {
		return result;
	}


	public static void main(String[] args) throws IOException {
		String canonicalName = Segment.class.getCanonicalName().replace('.','/');
		final NativeModule implementation = Driver.compileInMemory(loadResource(CCompiledQuery.class, "sampleQuery.cpp"));

		System.out.println(implementation.getErrors());
		CCompiledQuery compiled = new CCompiledQuery(implementation);
		compiled.init();
		Segment segment = new Segment(1);
		segment.rawData[0] = new long[10];
		compiled.process(segment);

		System.out.println("compiled.getResult() = " + compiled.getResult());;
	}

	/** Load the a resource file as string using the specified class' classloader. */
	public static String loadResource(Class aClass, String name) throws IOException {
		final StringWriter sw = new StringWriter();
		try (final InputStreamReader in = new InputStreamReader(aClass.getResourceAsStream(name))) {
			char[] buffer = new char[4096];
			int count;
			while ( (count = in.read(buffer)) != -1 ) {
				sw.write(buffer, 0, count);
			}
		}
		return sw.toString();
	}

}
