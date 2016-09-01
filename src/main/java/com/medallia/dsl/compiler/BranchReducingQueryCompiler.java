package com.medallia.dsl.compiler;

import com.medallia.codegen.JavaCodeGenerator;
import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.Query;
import com.medallia.dsl.ast.InExpr;

public class BranchReducingQueryCompiler<T> extends QueryCompiler<T> {

	public BranchReducingQueryCompiler(Query<T> query, DataSet dataSet) {
		super(query, dataSet);
	}

	@Override
	protected String generateInExpr(JavaCodeGenerator cg, InExpr inExpr) {
		final FieldDefinition fieldDef = dataSet.getFieldByName(inExpr.getFieldName());
		final FieldSpec fieldSpec = fieldDef.getFieldSpec();
		final long numValues = fieldSpec.getBound() - fieldSpec.getOrigin();
		final long[] inValues = inExpr.getValues();
		if (numValues < 64) {
			long mask = 0;
			for (long value : inValues) {
				mask |= 0x8000_0000_0000_0000L >>> (value - fieldSpec.getOrigin());
			}
			if (fieldSpec.getOrigin() == 0) {
				return String.format("(0x%xL << rawData[%d][row]) < 0", mask, fieldDef.getColumn());
			} else {
				return String.format("(0x%xL << (rawData[%d][row]-%dL)) < 0", mask, fieldDef.getColumn(), fieldSpec.getOrigin());
			}
		} else if (numValues < 100_000) {

		} else {
			// Fall back to a set
		}
		return super.generateInExpr(cg, inExpr);
	}
}
