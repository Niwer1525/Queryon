package niwer.queryon.tables;

import java.lang.reflect.Field;

import niwer.queryon.tables.api.IColumnField;

/**
 * Represents the supported SQL column types for table creation and manipulation.
 * Each enum constant corresponds to a specific SQL data type.
 */
public enum EnumColumnTypes {
    ENUM("TEXT", Enum.class), // Enum does not exist directly in SQL. We'll use CHECK (column IN ('value1', 'value2', ...)) constraints to simulate enum behavior.

    TEXT("TEXT", String.class),
    VARCHAR("VARCHAR"), // No class, this is handled by checking the charLimit in the ColumnField annotation

    INT("INTEGER", Integer.class, int.class),
    REAL("REAL", Double.class, double.class, Float.class, float.class),
    BOOLEAN("BOOLEAN", Boolean.class, boolean.class),
    DATE("DATE", java.sql.Date.class),
    DATE_TIME("DATETIME", java.sql.Timestamp.class);

    private final String sqlType;
    private final Class<?>[] javaTypes;

    EnumColumnTypes(String sqlType, Class<?>... javaTypes) {
        this.sqlType = sqlType;
        this.javaTypes = javaTypes;
    }

    public final String sql() { return this.sqlType; }

    /**
     * Gets the corresponding EnumColumnTypes for a given Java field based on its type.
     * 
     * @param field The Java field to determine the column type for
     * @return The corresponding EnumColumnTypes for the field's type
     * @throws IllegalArgumentException if the field's type is not supported
     */
    public final static EnumColumnTypes fromJava(Field field) {
        final Class<?> FIELD_TYPE = field.getType();
        if (field.isAnnotationPresent(IColumnField.class)) {
            final IColumnField ANNOTATION = field.getAnnotation(IColumnField.class);
            if (ANNOTATION.charLimit() > 0 && field.getType() == String.class) return VARCHAR; // If charLimit is specified, treat it as VARCHAR
        }
        return fromJava(FIELD_TYPE);
    }

    /**
    * Gets the corresponding EnumColumnTypes for a given Java type.
    * 
    * @param javaType The Java type to determine the column type for
    * @return The corresponding EnumColumnTypes for the Java type
    * @throws IllegalArgumentException if the Java type is not supported
    */
    public final static EnumColumnTypes fromJava(Class<?> javaType) {
        for (final EnumColumnTypes TYPE : values()) {
            for (final Class<?> SUPPORTED_TYPE : TYPE.javaTypes) {
                if (SUPPORTED_TYPE.isAssignableFrom(javaType)) return TYPE;
            }
        }
        throw new IllegalArgumentException("Unsupported Java type: " + javaType);
    }
}
