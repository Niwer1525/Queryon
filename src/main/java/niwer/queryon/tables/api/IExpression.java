package niwer.queryon.tables.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining an expression on a column, which can be used for default values, constraints, or other expression-based configurations.
 * The expression consists of a column, a condition, and optional values depending on the condition type.
 * 
 * @author Niwer
 */
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IExpression {
	String column();

	EnumExpressionCondition condition() default EnumExpressionCondition.GREATER_THAN; // Optional condition for the expression (e.g. IS NULL, IS NOT NULL, >, <, etc.)

	String[] values() default {}; // Optional array of values for the expression (e.g. for conditions like IN, NOT IN, etc.)
}