package com.medallia.dsl.nodes;

/** Visitor interface for expressions */
public interface ExprVisitor<T> {
	T visit(AndExpr andExpr);

	T visit(InExpr inExpr);

	T visit(NotExpr notExpr);

	T visit(OrExpr orExpr);
}
