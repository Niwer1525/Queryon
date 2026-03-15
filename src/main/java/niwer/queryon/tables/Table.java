package niwer.queryon.tables;

import java.util.LinkedHashSet;
import java.util.Set;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.queries.InteractionManager;

/**
 * Represents a database table that can be registered with a DataBase instance.
 * 
 * @see Table#createTable(String)
 * @see Table#createColumn(String, EnumColumnTypes)
 * @see Table#createTextColumn(String, int)
 * 
 * @author Niwer
 */
public abstract class Table {

    private final DataBase DATA_BASE;
    private final Set<Column> COLUMNS = new LinkedHashSet<>();
    
    public Table(DataBase db) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (this.name() == null || this.name().isEmpty()) throw new IllegalArgumentException("Table name cannot be null or empty.");
        this.DATA_BASE = db;
    }

    public abstract String name();
    
    public Set<Column> columns() { return Set.copyOf(COLUMNS); }

    /**
     * Helper method to create a column definition for the table. It supports basic column types (INT, VARCHAR, BOOLEAN) and allows to set various constraints (NOT NULL, UNIQUE, AUTO_INCREMENT, PRIMARY KEY) and default values.
     * @param name The name of the column
     * @param type The type of the column (INT, VARCHAR, BOOLEAN)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected static Column createColumn(DataBase db, String name, EnumColumnTypes type) {
        return new Column(db, name, type, 0, null);
    }

    /**
     * Helper method to create a VARCHAR column definition for the table. It allows to specify the size of the VARCHAR column and supports the same constraints and default values as createColumn.
     * @param name The name of the column
     * @param size The size of the VARCHAR column (maximum number of characters)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected static Column createColumn(DataBase db, String name, int size) { 
        return new Column(db, name, EnumColumnTypes.VARCHAR, size, null);
    }

    /**
     * Helper method to create an ENUM column definition for the table. It allows to specify the enum type that defines the possible values for the column, and supports the same constraints and default values as createColumn.
     * 
     * @param db The DataBase instance to which the column belongs
     * @param name The name of the column
     * @param enumType The enum type that defines the possible values for the column
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected static Column createColumn(DataBase db, String name, Class<? extends Enum<?>> enumType) {
        return new Column(db, name, EnumColumnTypes.ENUM, 0, enumType);
    }

    /**
     * Dropp all rows from this table
     * This is useful for resetting the state of the table during testing or when you want to clear all data without deleting the table itself.
     * 
     * @param db The DataBase instance to which the table belongs
     * @return The Table instance for chaining
     */
    public Table dropTable(DataBase db) {
        Console.log("Dropping table " + this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        InteractionManager.query(db, "DROP TABLE IF EXISTS " + this.name() + ";");
        return this;
    }

    /**
     * Add a column to the table definition (Use Table.createColumn or Table.createTextColumn to create a column)
     * @param column The column to add
     * @return The SQLTable instance for chaining
     */
    public Table addColumn(Column column) {
        COLUMNS.add(column);
        return this;
    }

    /**
     * Add multiple columns to the table definition (Use Table.createColumn or Table.createTextColumn to create columns)
     * @param columns The columns to add
     * @return The SQLTable instance for chaining
     */
    public Table addColumns(Column... columns) {
        for (final Column COLUMN : columns) COLUMNS.add(COLUMN);
        return this;
    }

    private boolean columnExists(String columnName) { //TODO
        return InteractionManager.queryInt(this.DATA_BASE, "SELECT COUNT(*) FROM pragma_table_info('" + this.name() + "') WHERE name = '" + columnName + "';") == 1;
    }

    /**
     * Execute the SQL command to create the table in the database with the defined columns.
     * This should be called after defining all columns for the table.
     */
    public void execute() {
        // if (this.DATA_BASE.tabExists(this)) { //TODO
        //     throw new IllegalStateException("Table '" + this.name() + "' already exists in the database.");
        //     // return;
        // }

        final StringBuilder QUERY = new StringBuilder("CREATE TABLE IF NOT EXISTS " + this.name());

        /* Get the columns */
        final String COLUMNS_SQL = COLUMNS.stream()
            .map(Column::sql)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Get foreign key constraints */
        final String CONSTRAINTS_SQL = COLUMNS.stream()
            .map(column -> column.constraintSQL())
            .filter(constraint -> constraint != null)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Build the final query */
        QUERY.append(" (").append(COLUMNS_SQL);
        if (!COLUMNS_SQL.isEmpty() && !CONSTRAINTS_SQL.isEmpty()) QUERY.append(", ");
        QUERY.append(CONSTRAINTS_SQL);
        QUERY.append(")");

        /* Execute the query */
        InteractionManager.query(this.DATA_BASE, QUERY.toString());
    }

    @Override public int hashCode() { return name().hashCode(); }
}