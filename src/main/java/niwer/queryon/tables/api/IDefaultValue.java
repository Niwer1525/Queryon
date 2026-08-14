package niwer.queryon.tables.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a default value for a column in a SQLSerializable class.
 * 
 * @author Niwer
 */
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IDefaultValue {

    String value() default ""; // Optional: specify a default value for the column (default is no default value)

    //TODO add support for CHECK constraints in the future, but for now, we will only support default values.
}
