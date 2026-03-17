package niwer.queryon.tables.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for grouping multiple expressions together with a logical operator (AND/OR).
 * This can be used to create complex default value conditions or other expression-based configurations.
 * 
 * @author Niwer
 */
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IExpressionGroup {

    EnumLogicOperator operator(); // The logical operator to combine the expressions (AND/OR)

    IExpression[] expressions();
}
