package com.medallia.dsl;

import com.medallia.dsl.nodes.AndExpr;
import com.medallia.dsl.nodes.Expr;
import com.medallia.dsl.nodes.InExpr;
import com.medallia.dsl.nodes.NotExpr;
import com.medallia.dsl.nodes.OrExpr;

import java.time.LocalDate;
import java.time.Period;
import java.util.function.Supplier;

public class ConditionalExpression {
	private final Supplier<Expr> nodeBuilder;

	private ConditionalExpression(Supplier<Expr> nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public ConditionalExpression and(ConditionalExpression other) {
		return new ConditionalExpression(() -> new AndExpr(nodeBuilder.get(), other.nodeBuilder.get()));
	}

	public ConditionalExpression or(ConditionalExpression other) {
		return new ConditionalExpression(() -> new OrExpr(nodeBuilder.get(), other.nodeBuilder.get()));
	}

	public static ConditionalExpression not(ConditionalExpression other) {
		return new ConditionalExpression(() -> new NotExpr(other.nodeBuilder.get()));
	}

	public static TimeFieldExpressionBuilder timeField(String fieldName) {
		return new TimeFieldExpressionBuilder(fieldName);
	}

	public static <T> FieldExpressionBuilder<T> field(String fieldName, Class<T> fieldType) {
		return new FieldExpressionBuilder<>(fieldName, fieldType);
	}

	public static class FieldExpressionBuilder<T> {
		private final String fieldName;
		private final Class<T> fieldType;

		FieldExpressionBuilder(String fieldName, Class<T> fieldType) {
			this.fieldName = fieldName;
			this.fieldType = fieldType;
		}

		public final ConditionalExpression is(T value) {
			return in(value);
		}

		@SafeVarargs
		public final ConditionalExpression in(T... values) {
			return new ConditionalExpression(() -> new InExpr(fieldName, fieldType, values));
		}
	}

	public static class TimeFieldExpressionBuilder {
		private final String fieldName;

		TimeFieldExpressionBuilder(String fieldName) {
			this.fieldName = fieldName;
		}

		public final ConditionalExpression is(LocalDate value) {
			throw new UnsupportedOperationException();
		}

		public final ConditionalExpression in(Period value) {
			throw new UnsupportedOperationException();
		}
	}

}