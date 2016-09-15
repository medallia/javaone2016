package com.medallia.dsl.compiler;

import com.medallia.codegen.JavaCodeGenerator;
import com.medallia.data.DataSet;
import com.medallia.data.DataSet.FieldDefinition;
import com.medallia.data.FieldSpec;
import com.medallia.dsl.FieldStats;
import com.medallia.dsl.Query;
import com.medallia.dsl.ast.InExpr;

import static com.medallia.dsl.Aggregate.statsAggregate;
import static com.medallia.dsl.ConditionalExpression.field;
import static com.medallia.dsl.QueryBuilder.newQuery;

public class BranchReducingQueryCompiler<T> extends QueryCompiler<T> {

	public BranchReducingQueryCompiler(Query<T> query, DataSet dataSet) {
		super(query, dataSet);
	}

	@Override
	protected String generateInExpr(JavaCodeGenerator cg, InExpr inExpr) {
		final FieldDefinition fieldDef = dataSet.getFieldByName(inExpr.getFieldName());
		final FieldSpec fieldSpec = fieldDef.getFieldSpec();
		final int numValues = fieldSpec.getBound() - fieldSpec.getOrigin();
		final int[] inValues = inExpr.getValues();
		if (inValues.length > 3 && numValues < 64) {
			long mask = 0;
			for (int value : inValues) {
				mask |= 0x8000_0000_0000_0000L >>> (value - fieldSpec.getOrigin());
			}
			if (fieldSpec.getOrigin() == 0) {
				return String.format("(0x%xL << rawData[%d][row]) < 0", mask, fieldDef.getColumn());
			} else {
				return String.format("(0x%xL << (rawData[%d][row]-%dL)) < 0", mask, fieldDef.getColumn(), fieldSpec.getOrigin());
			}
		}
		// Other cases, fall back to the naive one
		return super.generateInExpr(cg, inExpr);
	}

	public static void main(String[] args) {
		DataSet dataSet = DataSet.makeRandomDataSet(
				1_000_000, // rows
				50_000,	   // segment size
				new FieldSpec("a", 0, 11),
				new FieldSpec("b", 0, 5),
				new FieldSpec("ltr", 0, 11)
		);

		Query<FieldStats> query =
				newQuery()
				.filter(field("a").in(1, 2, 3, 9).or(field("b").is(3)))
				.aggregate(statsAggregate("ltr"));

		BranchReducingQueryCompiler<FieldStats> compiler = new BranchReducingQueryCompiler<>(query, dataSet);

		CompiledQueryBase<FieldStats> compiledQuery = compiler.compile().get();

		dataSet.getSegments().forEach(compiledQuery::process);

		System.out.println(compiledQuery.getResult());

	}
}
