package niwer.queryon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import niwer.queryon.tables.api.IColumnField;

/**
 * This interface allows you to create objects that will represent rows in the database.
 * 
 * @note Classes that extend this must have a default constructor (no parameters) to allow instantiation during objectification.
 */
public abstract class SQLSerializable<T> {

    public SQLSerializable() {} // Default constructor for objectification

    public final void objectify(ResultSet resultSet) throws SQLException {
        for (final Entry<Field, IColumnField> ENTRY : fieldsWithAnnotation().entrySet()) {
            final Field FIELD = ENTRY.getKey();
            final IColumnField ANNOTATION = ENTRY.getValue();
            final String COLUMN_NAME = columnName(FIELD, ANNOTATION);
            
            try {
                if (FIELD.getType() == int.class || FIELD.getType() == Integer.class) FIELD.set(this, resultSet.getInt(COLUMN_NAME));
                else if (FIELD.getType() == String.class) FIELD.set(this, resultSet.getString(COLUMN_NAME));
                else if (FIELD.getType() == boolean.class || FIELD.getType() == Boolean.class) FIELD.set(this, resultSet.getBoolean(COLUMN_NAME));
                else if (FIELD.getType() == long.class || FIELD.getType() == Long.class) FIELD.set(this, resultSet.getLong(COLUMN_NAME));
                else if (FIELD.getType() == double.class || FIELD.getType() == Double.class) FIELD.set(this, resultSet.getDouble(COLUMN_NAME));
                else if (FIELD.getType() == float.class || FIELD.getType() == Float.class) FIELD.set(this, resultSet.getFloat(COLUMN_NAME));
                else if (FIELD.getType() == Date.class) FIELD.set(this, resultSet.getDate(COLUMN_NAME));
                else throw new UnsupportedOperationException("Unsupported field type " + FIELD.getType().getName() + " for field " + FIELD.getName());
            } catch (final SQLException EX) {
                throw new RuntimeException("Failed to get value for column " + COLUMN_NAME + " during objectification of " + this.getClass().getName(), EX);
            } catch (final IllegalAccessException EX) {
                throw new RuntimeException("Failed to access field " + FIELD.getName() + " during objectification of " + this.getClass().getName(), EX);
            }
        }
    }

    /**
     * @return An array of all the column names from the fields of this class that are annotated with @IColumnField.
     * The column name is determined by the annotation's name() value if it's not empty, otherwise it defaults to the field name.
     */
    public final String[] columnNames() {
        return fieldsWithAnnotation().entrySet().stream()
            .map(e -> columnName(e.getKey(), e.getValue()))
            .toArray(String[]::new);
    }
    
    /**
     * @return An array of all the values from the fields of this class that are annotated with @IColumnField, in the same order as the column names. This is used for getting the values to insert into the database.
     */
    public final Object[] valuesFromObject() {
        return fieldsWithAnnotation().entrySet().stream()
            .map(e -> {
                try {
                    return e.getKey().get(this);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Failed to access field " + e.getKey().getName() + " during value extraction", ex);
                }
            })
            .toArray();
    }

    private static final String columnName(Field field, IColumnField annotation) { return annotation.name().isEmpty() ? field.getName() : annotation.name(); }

    private Map<Field, IColumnField> fieldsWithAnnotation() {
        final Map<Field, IColumnField> FIELDS = new LinkedHashMap<>();
        for (final Field FIELD : this.getClass().getDeclaredFields()) {
            FIELD.setAccessible(true);
            if (FIELD.isAnnotationPresent(IColumnField.class)) FIELDS.put(FIELD, FIELD.getAnnotation(IColumnField.class));
        }
        return FIELDS;
    }

    /**
     * Retrieves the default value of a field, used for getting default values for columns during insertion.
     * 
     * @param field The field to retrieve the value from
     * @return The value of the field
     * @throws RuntimeException if the field cannot be accessed
     */
    public static final Object defaultDataFromField(Field field) { // Data
        if (field == null) throw new IllegalArgumentException("Field cannot be null.");
        try {
            field.setAccessible(true);
            
            Object target = null;
            final boolean IS_STATIC = Modifier.isStatic(field.getModifiers());
            if (!IS_STATIC) {
                final var CONSTRUCTOR = field.getDeclaringClass().getDeclaredConstructor();
                CONSTRUCTOR.setAccessible(true);
                target = CONSTRUCTOR.newInstance();
            }
            
            return field.get(target);
        } catch (final ReflectiveOperationException EX) {
            throw new RuntimeException("Failed to access field " + field.getName() + " during retrieval of default value for column", EX);
        }
    }
}