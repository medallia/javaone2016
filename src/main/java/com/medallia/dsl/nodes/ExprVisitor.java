package com.medallia.dsl.nodes;

/** Visitor interface for expressions */
public interface ExprVisitor {
	default void visit(AndExpr andExpr) {}

	default void visit(InExpr inExpr) {}

	default void visit(NotExpr notExpr) {}

	default void visit(OrExpr orExpr) {}

	default void visit(TimeInExpr timeInExpr) {}

	default void visit(TimeIsExpr timeIsExpr) {}
}
