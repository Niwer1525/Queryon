package niwer.queryon.tables;

/**
 * Represents the supported SQL column types for table creation and manipulation.
 * Each enum constant corresponds to a specific SQL data type.
 */
public enum EnumColumnTypes {
    INT("INT"),
    VARCHAR("VARCHAR"),
    BOOLEAN("BOOLEAN");

    private final String sqlType;

    EnumColumnTypes(String sqlType) { this.sqlType = sqlType; }

    public String sqlType() { return sqlType; }
}
