package com.medallia.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataSet {
	private final Map<String, FieldDefinition> fieldsByName;
	private List<FieldDefinition> fields;
	private List<Segment> segments;

	private DataSet(List<FieldSpec> fields, List<Segment> segments) {
		this.fields = new ArrayList<>();
		for (int col = 0; col < fields.size(); col++) {
			this.fields.add(new FieldDefinition(col, fields.get(col)));
		}

		this.segments = segments;
		this.fieldsByName = this.fields.stream()
				.collect(Collectors.toMap(f -> f.getFieldSpec().getName(), Function.identity()));
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public FieldDefinition getFieldByName(String name) {
		return fieldsByName.get(name);
	}

	public static class FieldDefinition {
		private final int column;
		private final FieldSpec fieldSpec;

		public FieldDefinition(int column, FieldSpec fieldSpec) {
			this.column = column;
			this.fieldSpec = fieldSpec;
		}

		public int getColumn() {
			return column;
		}

		public FieldSpec getFieldSpec() {
			return fieldSpec;
		}
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
			segment.rawData[col] = rng.ints(segmentSize, field.getOrigin(), field.getBound()).toArray();
		}
		return segment;
	}
}
