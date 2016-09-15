package com.medallia.dsl;

import com.medallia.dsl.ast.AndExpr;
import com.medallia.dsl.ast.ConstantExpr;
import com.medallia.dsl.ast.Expr;
import com.medallia.dsl.ast.InExpr;
import com.medallia.dsl.ast.NotExpr;
import com.medallia.dsl.ast.OrExpr;

import java.util.function.Supplier;

public class ConditionalExpression {
	private final Supplier<Expr> nodeBuilder;

	private ConditionalExpression(Supplier<Expr> nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public Expr buildTree() { return nodeBuilder.get(); }

	public ConditionalExpression and(ConditionalExpression other) {
		return new ConditionalExpression(() -> new AndExpr(nodeBuilder.get(), other.nodeBuilder.get()));
	}

	public ConditionalExpression or(ConditionalExpression other) {
		return new ConditionalExpression(() -> new OrExpr(nodeBuilder.get(), other.nodeBuilder.get()));
	}

	public static ConditionalExpression not(ConditionalExpression other) {
		return new ConditionalExpression(() -> new NotExpr(other.nodeBuilder.get()));
	}

	public static FieldExpressionBuilder field(String fieldName) {
		return new FieldExpressionBuilder(fieldName);
	}

	public static ConditionalExpression constant(boolean value) {
		return new ConditionalExpression(() -> new ConstantExpr(value));
	}

	public static class FieldExpressionBuilder {
		private final String fieldName;

		FieldExpressionBuilder(String fieldName) {
			this.fieldName = fieldName;
		}

		public final ConditionalExpression is(int value) {
			return in(value);
		}

		public final ConditionalExpression in(int... values) {
			return new ConditionalExpression(() -> new InExpr(fieldName, values));
		}
	}
}