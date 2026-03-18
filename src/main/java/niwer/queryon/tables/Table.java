package niwer.queryon.tables;

import java.util.LinkedHashSet;
import java.util.Set;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.api.IColumnField;

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
    private final Set<Expression> CHECK_CONSTRAINTS = new LinkedHashSet<>();
    
    public Table(DataBase db) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (this.name() == null || this.name().isEmpty()) throw new IllegalArgumentException("Table name cannot be null or empty.");
        this.DATA_BASE = db;
    }

    public abstract String name();
    
    public final Set<Column> columns() { return Set.copyOf(COLUMNS); }

    /**
     * Helper method to create a column definition for the table. It supports basic column types (INT, VARCHAR, BOOLEAN) and allows to set various constraints (NOT NULL, UNIQUE, AUTO_INCREMENT, PRIMARY KEY) and default values.
     * @param name The name of the column
     * @param type The type of the column (INT, VARCHAR, BOOLEAN)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected final static Column createColumn(DataBase db, String name, EnumColumnTypes type) {
        return new Column(db, name, type, 0, null);
    }

    /**
     * Helper method to create a VARCHAR column definition for the table. It allows to specify the size of the VARCHAR column and supports the same constraints and default values as createColumn.
     * @param name The name of the column
     * @param size The size of the VARCHAR column (maximum number of characters)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected final static Column createColumn(DataBase db, String name, int size) { 
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
    protected final static Column createColumn(DataBase db, String name, Class<? extends Enum<?>> enumType) {
        return new Column(db, name, EnumColumnTypes.ENUM, 0, enumType);
    }

    /**
     * Drop this table
     * This is useful for resetting the state of the table during testing or when you want to clear all data without deleting the table itself.
     * 
     * @param db The DataBase instance to which the table belongs
     * @return The Table instance for chaining
     */
    protected final Table dropTable() {
        Console.log("Dropping table " + this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        QueryManager.query(this.DATA_BASE, "DROP TABLE IF EXISTS " + this.name() + ";");
        return this;
    }

    /**
     * Drop all rows from this table without dropping the table itself. This is useful for resetting the state of the table during testing or when you want to clear all data without deleting the table structure.
     * 
     * @param db The DataBase instance to which the table belongs
     * @return The Table instance for chaining
     */
    public final Table dropAllRows() {
        Console.log("Dropping all rows from table " + this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        QueryManager.query(this.DATA_BASE, "DELETE FROM " + this.name() + ";");
        return this;
    }

    /**
     * Add a column to the table definition (Use Table.createColumn or Table.createTextColumn to create a column)
     * @param column The column to add
     * @return The SQLTable instance for chaining
     */
    public final Table addColumn(Column column) {
        COLUMNS.add(column);
        return this;
    }

    /**
     * Add multiple columns to the table definition (Use Table.createColumn or Table.createTextColumn to create columns)
     * @param columns The columns to add
     * @return The SQLTable instance for chaining
     */
    public final Table addColumns(Column... columns) {
        for (final Column COLUMN : columns) COLUMNS.add(COLUMN);
        return this;
    }

    /**
     * Add columns to the table definition based on the fields of a class annotated with @ColumnField.
     * Each field in the class that is annotated with @ColumnField will be converted into a column definition and added to the table. This allows for defining table columns using simple Java classes, which can be more convenient and less error-prone than manually creating Column instances for each column.
     * 
     * @param clazz The class containing the @ColumnField annotations
     * @return The Table instance for chaining
     */
    public final Table addColumnsFromClass(Class<? extends SQLSerializable<?>> clazz) {
        if (clazz == null) throw new IllegalArgumentException("Class cannot be null.");
        
        for (final var FIELD : clazz.getDeclaredFields()) {
            if (FIELD.isAnnotationPresent(IColumnField.class)) {
                final IColumnField ANNOTATION = FIELD.getAnnotation(IColumnField.class);
                this.addColumn(new Column(this.DATA_BASE, FIELD, ANNOTATION));
            }
        }
        return this;
    }

    public final Table addCheckConstraint(Expression expression) {
        CHECK_CONSTRAINTS.add(expression);
        return this;
    }

    public final Table addCheckConstraints(Expression... expressions) {
        for (final Expression EXPRESSION : expressions) addCheckConstraint(EXPRESSION);
        return this;
    }

    private final boolean columnExists(Column column) {
        return columnExists(column.NAME);
    }

    private final boolean columnExists(String columnName) {
        return QueryManager.queryInt(DATA_BASE, "SELECT COUNT(*) FROM pragma_table_info('" + this.name() + "') WHERE name = '" + columnName + "';") > 0;
    }

    /**
     * Execute the SQL command to create the table in the database with the defined columns.
     * This should be called after defining all columns for the table.
     */
    public final void execute() {
        if (this.DATA_BASE.tabExists(this)) {
            final var MISSING_COLUMNS = COLUMNS.stream().filter(column -> !columnExists(column)).toList();

            /* If there are missing columns, alter the table */
            if (!MISSING_COLUMNS.isEmpty()) {
                final StringBuilder QUERY = new StringBuilder("");
                for (final Column MISSING_COLUMN : MISSING_COLUMNS) QUERY.append("ALTER TABLE ").append(this.name()).append(" ADD COLUMN ").append(MISSING_COLUMN.toString()).append("; ");
                QueryManager.query(this.DATA_BASE, QUERY.toString());
                return;
            }

            Console.log("Table %s already exists with all defined columns. Skipping creation.", this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
            return;
        }

        final StringBuilder QUERY = new StringBuilder("CREATE TABLE IF NOT EXISTS " + this.name());

        /* Get the columns */
        final String COLUMNS_SQL = COLUMNS.stream()
            .map(Column::toString)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Get foreign key constraints */
        final String CONSTRAINTS_SQL = COLUMNS.stream()
            .map(column -> column.constraintSQL())
            .filter(constraint -> constraint != null)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Get check constraints */
        final String CHECK_CONSTRAINTS_SQL = CHECK_CONSTRAINTS.stream()
            .map(Expression::toString)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Build the final query */
        QUERY.append(" (").append(COLUMNS_SQL);
        if (!COLUMNS_SQL.isEmpty() && !CONSTRAINTS_SQL.isEmpty()) QUERY.append(", ");
        QUERY.append(CONSTRAINTS_SQL);
        if (!CHECK_CONSTRAINTS_SQL.isEmpty()) {
            QUERY.append(", ");
            QUERY.append("CHECK (").append(CHECK_CONSTRAINTS_SQL).append(")");
        }
        QUERY.append(")");

        /* Execute the query */
        QueryManager.query(this.DATA_BASE, QUERY.toString());
    }

    @Override public int hashCode() { return name().hashCode(); }
}