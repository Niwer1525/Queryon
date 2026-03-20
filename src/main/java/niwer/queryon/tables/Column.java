package niwer.queryon.tables;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.Expression;
import niwer.queryon.tables.api.IColumnField;

/**
 * Represents a column in a database table, including its name, type, constraints, and other properties.
 * 
 * @author Niwer
 */
@SuppressWarnings("rawtypes")
public class Column {
    private static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    protected final String ESCAPED_NAME;
    private final DataBase DATA_BASE;
    private final EnumColumnTypes TYPE;
    private final int SIZE; // Only used for VARCHAR, ignored otherwise
    private final String[] ENUM_VALUES_NAMES; // Only used for ENUM, ignored otherwise
    
    private boolean autoIncrement = false;
    private boolean notNull = false;
    private boolean unique = false;
    private boolean primaryKey = false;
    private Object defaultValue = null;
    private Expression defaultValueExpression = null; // For default values that are expressions (e.g. CURRENT_TIMESTAMP)

    private Table foreignKeyReferenceTable = null;
    private String foreignKeyReferenceColumn = null;
    private EnumForeginKeyAction foreignKeyDeleteAction = EnumForeginKeyAction.NO_ACTION;

    protected Column(DataBase db, Field field, IColumnField annotation) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");

        this.DATA_BASE = db;
        this.ESCAPED_NAME = QueryonEngine.escapeString(annotation.name().isEmpty() ? field.getName() : annotation.name());
        this.SIZE = annotation.charLimit();
        this.TYPE = EnumColumnTypes.fromJava(field);
        if (this.TYPE == EnumColumnTypes.ENUM) this.ENUM_VALUES_NAMES = enumToStrings(field.getType().asSubclass(Enum.class));
        else this.ENUM_VALUES_NAMES = null;
        
        if (annotation.charLimit() > 0 && TYPE != EnumColumnTypes.VARCHAR) throw new IllegalArgumentException("charLimit is only applicable to VARCHAR columns for column " + ESCAPED_NAME);
        if (annotation.autoIncrement() && TYPE != EnumColumnTypes.INT) throw new IllegalArgumentException("autoIncrement is only applicable to INT columns for column " + ESCAPED_NAME);
        if (annotation.notNull()) notNull();
        if (annotation.unique()) unique();
        if (annotation.primaryKey()) primaryKey(); // This will also set notNull and unique to true
        if (annotation.autoIncrement()) autoIncrement();
        if (annotation.foreignKey().table() != Table.class) foreignKey(annotation.foreignKey().table(), annotation.foreignKey().column(), annotation.foreignKey().onDelete());
        
        final Object DEFAULT_VALUE = SQLSerializable.defaultDataFromField(field);
        if (DEFAULT_VALUE != null) defaultValue(DEFAULT_VALUE);
    }

    private static final String[] enumToStrings(Class<? extends Enum> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    protected Column(DataBase db, String name, EnumColumnTypes type, int size, Class<? extends Enum> enumType) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        if (type == null) throw new IllegalArgumentException("Column type cannot be null.");
        if (type == EnumColumnTypes.VARCHAR && size <= 0) throw new IllegalArgumentException("VARCHAR column size must be greater than 0.");
        if (type == EnumColumnTypes.ENUM && enumType == null) throw new IllegalArgumentException("ENUM column must have a non-null enum type.");

        this.DATA_BASE = db;
        this.ESCAPED_NAME = QueryonEngine.escapeString(name);
        this.TYPE = type;
        this.SIZE = size;
        if (type == EnumColumnTypes.ENUM) this.ENUM_VALUES_NAMES = Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toArray(String[]::new);
        else this.ENUM_VALUES_NAMES = null;
    }

    /**
     * Helper method to handle default value assignment for DATE and DATE_TIME columns.
     */
    private final void handleDateStringDefault(String string, String regex, String typeName) {
        if (string.equalsIgnoreCase(CURRENT_TIMESTAMP)) this.defaultValue = CURRENT_TIMESTAMP;
        else if (string.matches(regex)) this.defaultValue = string;
        else throw new IllegalArgumentException("Default value string does not match expected format for " + typeName + " column for column " + ESCAPED_NAME);
    }
    
    /**
     * Defines this column as auto-incrementing, meaning that its value will automatically increase for each new record inserted into the table.
     * When this method is called, it sets the autoIncrement flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public final Column autoIncrement() {
        this.autoIncrement = true;
        return this;
    }

    /**
     * Defines this column as NOT NULL, meaning that it cannot contain null values.
     * When this method is called, it sets the notNull flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public final Column notNull() {
        this.notNull = true;
        return this;
    }

    /**
     * Defines this column as unique, meaning that all values in this column must be distinct across the table.
     * When this method is called, it sets the unique flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public final Column unique() {
        this.unique = true;
        return this;
    }

    /**
     * Defines this column as a primary key.
     * A primary key is a unique identifier for each record in the table, and it cannot be null.
     * When this method is called, it sets the primaryKey flag to true and also applies the notNull and unique constraints to ensure that the column can serve as a valid primary key.
     * 
     * @return The SQLColumn instance for chaining
     */
    public final Column primaryKey() {
        this.primaryKey = true;
        return this.notNull().unique();
    }

    /**
     * Defines a foreign key constraint for this column, referencing a column in another table.
     * The referenceTable parameter specifies the name of the referenced table, and the referenceColumn parameter specifies the name of the referenced column in that table.
     * When this method is called, it sets the foreign key reference information for this column, which will be included in the SQL definition when the table is created.
     * 
     * @param referenceTable The name of the table that this column references as a foreign key
     * @param referenceColumn The name of the column in the referenced table that this column references as a foreign key
     * @return The SQLColumn instance for chaining
     */
    public final Column foreignKey(Class<? extends Table> referenceTable, String referenceColumn, EnumForeginKeyAction deleteAction) {
        this.foreignKeyReferenceTable = this.DATA_BASE.getTable(referenceTable);
        this.foreignKeyReferenceColumn = referenceColumn;
        this.foreignKeyDeleteAction = deleteAction;
        return this;
    }
    
    /**
     * Sets the default value for the column using an expression.
     * This is useful for setting default values that are not constant, such as the current timestamp.
     * 
     * @param value The default value to set. It can be a constant value or a special value like "CURRENT_TIMESTAMP" for DATE and DATE_TIME columns.
     * @param expression The expression that defines the default value. For example, for a "age" column, you could use an expression like "age < 18" to set the default value to "minor" if the age is less than 18, and "adult" otherwise.
     * @return The SQLColumn instance for chaining
     */
    public final Column defaultValue(Object value, Expression expression) {
        if (this.TYPE == EnumColumnTypes.ENUM)
            throw new IllegalArgumentException("Default value expressions are not supported for ENUM columns for column " + ESCAPED_NAME);
        
        this.defaultValueExpression = expression;
        return defaultValue(value);
    }

    /**
     * Sets the default value for the column.
     * The type of the default value must match the column type (Integer for INT, String for VARCHAR, Boolean for BOOLEAN, etc). If the type does not match, an IllegalArgumentException is thrown.
     * 
     * @param value The default value to set
     * @return The SQLColumn instance for chaining
     * 
     * @note If you want to set the default value to the current timestamp for DATE or DATE_TIME columns, you can pass the string "CURRENT_TIMESTAMP" as the value.
     * Or if you want to set it to a specific date or datetime, you can pass a java.util
     * Or if you want to set the date in string format, it must match the expected format for the column type (e.g. "YYYY-MM-DD" for DATE and "YYYY-MM-DD HH:MM:SS" for DATE_TIME).
     */
    public final Column defaultValue(Object value) {
        switch (this.TYPE) {
            case ENUM -> {
                if (!(value instanceof Enum))
                    throw new IllegalArgumentException("Default value type does not match column type for column " + ESCAPED_NAME);
                this.defaultValue = value;
            }
            case REAL -> this.defaultValue = (Double) value;
            case INT -> this.defaultValue = (Integer) value;
            case VARCHAR -> {
                final String STRING_VALUE = value.toString();
                if (STRING_VALUE.length() > SIZE) throw new IllegalArgumentException("Default value length exceeds VARCHAR column size for column " + ESCAPED_NAME);
                this.defaultValue = STRING_VALUE;
            }
            case TEXT -> this.defaultValue = value.toString();
            case BOOLEAN -> this.defaultValue = (Boolean) value;
            case DATE -> {
                if (value instanceof Date date) this.defaultValue = QueryonEngine.dateToSQL(date); // Dates are stored as strings in the format "YYYY-MM-DD"
                else if (value instanceof String string) handleDateStringDefault(string, "\\d{4}-\\d{2}-\\d{2}", "DATE");
                else throw new IllegalArgumentException("Default value type does not match column type for column " + ESCAPED_NAME);
            }
            case DATE_TIME -> {
                if (value instanceof Date date) this.defaultValue = QueryonEngine.dateTimeToSQL(date); // Datetimes are stored as strings in the format "YYYY-MM-DD HH:MM:SS"
                else if (value instanceof String string) handleDateStringDefault(string, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "DATE_TIME");
                else throw new IllegalArgumentException("Default value type does not match column type for column " + ESCAPED_NAME);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder QUERY = new StringBuilder(ESCAPED_NAME + " " + TYPE.sql());
        if (TYPE == EnumColumnTypes.VARCHAR) QUERY.append(String.format("(%d)", SIZE));
        if (autoIncrement) QUERY.append(" AUTO_INCREMENT");
        if (primaryKey) QUERY.append(" PRIMARY KEY");
        if (notNull) QUERY.append(" NOT NULL");
        if (unique) QUERY.append(" UNIQUE");
        if (defaultValue != null) {
            QUERY.append(String.format(" DEFAULT '%s'", defaultValue));
            if (TYPE == EnumColumnTypes.ENUM) QUERY.append(" CHECK (" + ESCAPED_NAME + " IN ('" + String.join("', '", ENUM_VALUES_NAMES) + "'))"); // For ENUM columns, we also add a CHECK constraint to ensure that the value is one of the defined enum values
            if (defaultValueExpression != null) QUERY.append(" CHECK (" + defaultValueExpression + ")");
        }
        return QUERY.toString();
    }

    /**
     * Builds a SQLite-safe column definition for ALTER TABLE ... ADD COLUMN.
     * SQLite does not allow adding UNIQUE, PRIMARY KEY, or AUTO_INCREMENT constraints this way.
     */
    protected final String toAlterColumnSQL() {
        final StringBuilder QUERY = new StringBuilder(ESCAPED_NAME + " " + TYPE.sql());
        if (TYPE == EnumColumnTypes.VARCHAR) QUERY.append(String.format("(%d)", SIZE));
        if (defaultValue != null) QUERY.append(String.format(" DEFAULT '%s'", defaultValue));
        return QUERY.toString();
    }

    protected final String constraintSQL() {
        if (foreignKeyReferenceTable == null || foreignKeyReferenceColumn == null) return null;
        return String.format("FOREIGN KEY (%s) REFERENCES %s(%s)%s", ESCAPED_NAME, foreignKeyReferenceTable.escapedName(), foreignKeyReferenceColumn, " ON DELETE " + foreignKeyDeleteAction);
    }

    @Override public int hashCode() { return ESCAPED_NAME.hashCode(); }
}
