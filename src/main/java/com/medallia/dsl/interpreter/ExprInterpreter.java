package com.medallia.dsl.interpreter;

import com.medallia.data.DataSet;
import com.medallia.data.Segment;
import com.medallia.dsl.nodes.AndExpr;
import com.medallia.dsl.nodes.Expr;
import com.medallia.dsl.nodes.ExprVisitor;
import com.medallia.dsl.nodes.InExpr;
import com.medallia.dsl.nodes.NotExpr;
import com.medallia.dsl.nodes.OrExpr;

import java.util.stream.LongStream;

public class ExprInterpreter
{
	private final Expr expression;

	public ExprInterpreter(Expr expression) {
		this.expression = expression;
	}

	public boolean eval(DataSet dataSet, Segment segment, int row) {
		return expression.visit(new ExprVisitor<Boolean>() {

			@Override
			public Boolean visit(AndExpr andExpr) {
				return andExpr.getLeft().visit(this) && andExpr.getRight().visit(this);
			}

			@Override
			public Boolean visit(InExpr inExpr) {
				return LongStream.of(inExpr.getValues())
						.filter(v -> v == segment.rawData[dataSet.getFieldByName(inExpr.getFieldName()).getColumn()][row])
						.findAny()
						.isPresent();
			}

			@Override
			public Boolean visit(NotExpr notExpr) {
				return !notExpr.getTarget().visit(this);
			}

			@Override
			public Boolean visit(OrExpr orExpr) {
				return orExpr.getLeft().visit(this) || orExpr.getRight().visit(this);
			}
		});
	}
}
