package niwer.queryon.tables;

public class SQLColumn {
    private final String NAME;
    private final EnumColumnTypes TYPE;
    private final int SIZE; // Only used for VARCHAR, ignored otherwise
    
    private boolean autoIncrement = false;
    private boolean notNull = false;
    private boolean unique = false;
    private boolean primaryKey = false;
    private Object defaultValue = null;

    private String foreignKeyReferenceTable = null;
    private String foreignKeyReferenceColumn = null;

    protected SQLColumn(String name, EnumColumnTypes type, int size) {
        this.NAME = name;
        this.TYPE = type;
        this.SIZE = size;
    }
    
    /**
     * Defines this column as auto-incrementing, meaning that its value will automatically increase for each new record inserted into the table.
     * When this method is called, it sets the autoIncrement flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public SQLColumn autoIncrement() {
        this.autoIncrement = true;
        return this;
    }

    /**
     * Defines this column as NOT NULL, meaning that it cannot contain null values.
     * When this method is called, it sets the notNull flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public SQLColumn notNull() {
        this.notNull = true;
        return this;
    }

    /**
     * Defines this column as unique, meaning that all values in this column must be distinct across the table.
     * When this method is called, it sets the unique flag to true, which will be included in the SQL definition when the table is created.
     * 
     * @return The SQLColumn instance for chaining
     */
    public SQLColumn unique() {
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
    public SQLColumn primaryKey() {
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
    public SQLColumn foreignKey(String referenceTable, String referenceColumn) {
        this.foreignKeyReferenceTable = referenceTable;
        this.foreignKeyReferenceColumn = referenceColumn;
        return this;
    }
    
    /**
     * Sets the default value for the column.
     * The type of the default value must match the column type (Integer for INT, String for VARCHAR, Boolean for BOOLEAN). If the type does not match, an IllegalArgumentException is thrown.
     * 
     * @param value The default value to set
     * @return The SQLColumn instance for chaining
     */
    public SQLColumn defaultValue(Object value) {
        try {
            switch (TYPE) {
                case INT -> this.defaultValue = (Integer) value;
                case VARCHAR -> this.defaultValue = value.toString(); //TODO Check if the string is too long for the column size and throw an exception if it is
                case BOOLEAN -> this.defaultValue = (Boolean) value;
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Default value type does not match column type for column " + NAME);
        }
        return this;
    }

    protected String sql() { //TODO Rewrite this method to be more efficient and readable
        final StringBuilder DEFINITION = new StringBuilder(NAME + " " + TYPE.sqlType());
        if (TYPE == EnumColumnTypes.VARCHAR) DEFINITION.append("(").append(SIZE).append(")");
        if (autoIncrement) DEFINITION.append(" AUTO_INCREMENT");
        if (notNull) DEFINITION.append(" NOT NULL");
        if (unique) DEFINITION.append(" UNIQUE");
        if (defaultValue != null) DEFINITION.append(" DEFAULT '").append(defaultValue).append("'");
        if (foreignKeyReferenceTable != null && foreignKeyReferenceColumn != null) {
            DEFINITION.append(" REFERENCES ").append(foreignKeyReferenceTable).append("(").append(foreignKeyReferenceColumn).append(")");
        }
        return DEFINITION.toString();
    }

    @Override
    public int hashCode() {
        return NAME.hashCode()
            + TYPE.hashCode()
            + Boolean.hashCode(this.autoIncrement)
            + Boolean.hashCode(this.notNull)
            + Boolean.hashCode(this.unique)
            + Boolean.hashCode(this.primaryKey)
            + (defaultValue != null ? defaultValue.hashCode() : 0)
        ;
    }
}
