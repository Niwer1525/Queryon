package niwer.queryon.tables;

/**
 * Represents the supported SQL column types for table creation and manipulation.
 * Each enum constant corresponds to a specific SQL data type.
 */
public enum EnumColumnTypes {
    ENUM("TEXT"), // Enum does not exist directly in SQL. We'll use CHECK (column IN ('value1', 'value2', ...)) constraints to simulate enum behavior.

    INT("INTEGER"),
    REAL("REAL"),
    VARCHAR("VARCHAR"),
    TEXT("TEXT"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    DATE_TIME("DATETIME");

    private final String sqlType;

    EnumColumnTypes(String sqlType) { this.sqlType = sqlType; }

    public String sqlType() { return sqlType; }
}
