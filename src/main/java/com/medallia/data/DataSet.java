package com.medallia.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataSet {
	private final Map<String, FieldSpec> fieldsByName;
	private List<FieldSpec> fields;
	private List<Segment> segments;

	private DataSet(List<FieldSpec> fields, List<Segment> segments) {
		this.fields = fields;
		this.segments = segments;
		this.fieldsByName = fields.stream()
				.collect(Collectors.toMap(FieldSpec::getName, Function.identity()));
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public List<FieldSpec> getFields() {
		return fields;
	}

	public FieldSpec getFieldByName(String name) {
		return fieldsByName.get(name);
	}

	public static DataSet makeRandomDataSet(int rows, int segmentSize, FieldSpec... fields) {
		final Random rng = new Random();
		final List<Segment> segments = new ArrayList<>();
		final int nSegments = rows / segmentSize;
		for (int i = 0; i < nSegments; i++) {
			segments.add(makeRandomSegment(rng, fields, segmentSize));
		}
		segments.add(makeRandomSegment(rng, fields, rows % segmentSize));
		return new DataSet(Arrays.asList(fields), segments);
	}

	private static Segment makeRandomSegment(Random rng, FieldSpec[] fields, int segmentSize) {
		Segment segment = new Segment(fields.length);
		for (int col = 0; col < fields.length; col++) {
			final FieldSpec field = fields[col];
			segment.rawData[col] = rng.longs(segmentSize, field.getMinValue(), field.getMaxValue()).toArray();
		}
		return segment;
	}
}
