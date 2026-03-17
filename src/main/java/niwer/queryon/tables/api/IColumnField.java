package niwer.queryon.tables.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import niwer.queryon.tables.Table;

/**
 * Annotation to mark a field in a SQLSerializable class as corresponding to a column in the database.
 * 
 * @author Niwer
 */
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IColumnField {

    String name() default ""; // Optional: specify the column name if it differs from the field name

    int charLimit() default 0; // Optional: specify a character limit for string types (VARCHAR).

    boolean autoIncrement() default false; // Optional: specify if the column should be AUTO_INCREMENT (for integer types)
    
    boolean notNull() default false; // Optional: specify if the column should be NOT NULL

    boolean unique() default false; // Optional: specify if the column should be UNIQUE

    boolean primaryKey() default false; // Optional: specify if the column should be a PRIMARY KEY

    IForeignKey foreignKey() default @IForeignKey(table = Table.class, column = ""); // Optional: specify a foreign key relationship (default is no foreign key)
    
    String defaultValue() default ""; // SQL literal value used as default (e.g. 25, 'text', TRUE)
}
