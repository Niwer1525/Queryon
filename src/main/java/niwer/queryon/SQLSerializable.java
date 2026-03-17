package niwer.queryon;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import niwer.queryon.tables.api.IColumnField;

/**
 * This interface allows you to create objects that will represent rows in the database.
 * 
 * @note Classes that extend this must have a default constructor (no parameters) to allow instantiation during objectification.
 */
public abstract class SQLSerializable<T> {

    public SQLSerializable() {} // Default constructor for objectification

    public final T objectify(ResultSet resultSet) throws SQLException {
        final Map<Field, IColumnField> FIELDS = getFieldsWithAnnotation();
        for (final Map.Entry<Field, IColumnField> ENTRY : FIELDS.entrySet()) {
            final Field FIELD = ENTRY.getKey();
            final IColumnField ANNOTATION = ENTRY.getValue();
            final String COLUMN_NAME = ANNOTATION.name().isEmpty() ? FIELD.getName() : ANNOTATION.name();
            
            try {
                if (FIELD.getType() == int.class || FIELD.getType() == Integer.class) FIELD.set(this, resultSet.getInt(COLUMN_NAME));
                else if (FIELD.getType() == String.class) FIELD.set(this, resultSet.getString(COLUMN_NAME));
                else if (FIELD.getType() == boolean.class || FIELD.getType() == Boolean.class) FIELD.set(this, resultSet.getBoolean(COLUMN_NAME));
                else if (FIELD.getType() == long.class || FIELD.getType() == Long.class) FIELD.set(this, resultSet.getLong(COLUMN_NAME));
                else if (FIELD.getType() == double.class || FIELD.getType() == Double.class) FIELD.set(this, resultSet.getDouble(COLUMN_NAME));
                else if (FIELD.getType() == float.class || FIELD.getType() == Float.class) FIELD.set(this, resultSet.getFloat(COLUMN_NAME));
                else throw new UnsupportedOperationException("Unsupported field type " + FIELD.getType().getName() + " for field " + FIELD.getName());
            } catch (final SQLException EX) {
                throw new RuntimeException("Failed to get value for column " + COLUMN_NAME + " during objectification of " + this.getClass().getName(), EX);
            } catch (final IllegalAccessException EX) {
                throw new RuntimeException("Failed to access field " + FIELD.getName() + " during objectification of " + this.getClass().getName(), EX);
            }
        }
        throw new UnsupportedOperationException("Objectification is not implemented yet for " + this.getClass().getName());
    }

    private Map<Field, IColumnField> getFieldsWithAnnotation() {
        final Map<Field, IColumnField> FIELDS = new HashMap<>();
        for (final Field FIELD : this.getClass().getDeclaredFields()) {
            FIELD.setAccessible(true);
            if (FIELD.isAnnotationPresent(IColumnField.class)) FIELDS.put(FIELD, FIELD.getAnnotation(IColumnField.class));
        }
        return FIELDS;
    }
}