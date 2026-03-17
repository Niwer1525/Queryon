package niwer.queryon.tables.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import niwer.queryon.tables.EnumForeginKeyAction;
import niwer.queryon.tables.Table;

/**
 * Annotation to specify a foreign key relationship for a column in a SQLSerializable class.
 * 
 * @author Niwer
 */
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IForeignKey {

    Class<? extends Table> table(); // The referenced table

    String column(); // The referenced column in the foreign table

    EnumForeginKeyAction onDelete() default EnumForeginKeyAction.NO_ACTION; // Action to take on delete (default: NO_ACTION)
}
