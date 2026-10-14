package niwer.queryon.tables;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonException;
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

    public final String escapedName() {
        return QueryonEngine.escapeString(this.name());
    }
    
    public final Set<Column> columns() { return Set.copyOf(COLUMNS); }

    /**
     * Helper method to create a column definition for the table. It supports basic column types (INT, VARCHAR, BOOLEAN) and allows to set various constraints (NOT NULL, UNIQUE, AUTOINCREMENT, PRIMARY KEY) and default values.
     * 
     * @param name The name of the column
     * @param type The type of the column (INT, VARCHAR, BOOLEAN)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected final static Column createColumn(DataBase db, String name, EnumColumnTypes type) {
        return new Column(db, name, type, 0, null);
    }

    /**
     * Helper method to create a VARCHAR column definition for the table. It allows to specify the size of the VARCHAR column and supports the same constraints and default values as createColumn.
     * 
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

    public final Set<String> getRegisteredColumnNames() {
        return COLUMNS.stream().map(Column::name).collect(Collectors.toCollection(LinkedHashSet::new));
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
        return QueryManager.queryInt(DATA_BASE, "SELECT COUNT(*) FROM pragma_table_info(" + this.escapedName() + ") WHERE name = " + column.ESCAPED_NAME + ";") > 0;
    }

    /**
     * Execute the SQL command to create the table in the database with the defined columns.
     * This should be called after defining all columns for the table.
     */
    public final void execute() {
        if (this.COLUMNS.isEmpty()) throw new IllegalStateException("Cannot create table '" + this.name() + "' without any columns.");

        final String DESIRED_COLUMNS_AND_CONSTRAINTS = buildColumnsAndConstraintsSQL();

        /* If the table exist, check if the schema matches the desired one */
        if (this.DATA_BASE.automaticSchemaSync() && this.DATA_BASE.tabExists(this)) {
            /* Handle add-only changes with ALTER TABLE before considering a rebuild. */
            final var MISSING_COLUMNS = COLUMNS.stream().filter(column -> !columnExists(column)).toList();
            if (!MISSING_COLUMNS.isEmpty()) {
                for (final Column MISSING_COLUMN : MISSING_COLUMNS) {
                    final String SQL = "ALTER TABLE " + this.name() + " ADD COLUMN " + MISSING_COLUMN.toAlterColumnSQL() + ";";
                    QueryManager.query(this.DATA_BASE, SQL);
                }
            }

            /* Fetch the actual CREATE statement currently stored in the SQLite file */
            String currentSchema = QueryManager.queryString(this.DATA_BASE, "SELECT sql FROM sqlite_master WHERE type='table' AND name=" + this.escapedName() + ";");
            String expectedCreatePrefix = "CREATE TABLE " + this.name() + " (" + DESIRED_COLUMNS_AND_CONSTRAINTS + ")";

            /* If the current schema does not match the Java model (e.g., CHECK constraint changed) */
            if (currentSchema != null && !QueryonEngine.normalizeSql(currentSchema).equals(QueryonEngine.normalizeSql(expectedCreatePrefix))) {
                migrateTable(DESIRED_COLUMNS_AND_CONSTRAINTS);
                return;
            }

            if (!MISSING_COLUMNS.isEmpty()) return;
            Console.log("Table %s already exists with all defined columns. Skipping creation.", this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
            return;
        }

        /* Execute the query for a fresh table */
        QueryManager.query(this.DATA_BASE, "CREATE TABLE IF NOT EXISTS " + this.name() + " (" + DESIRED_COLUMNS_AND_CONSTRAINTS + ")");
    }

    private String buildColumnsAndConstraintsSQL() {
        final String COLUMNS_SQL = COLUMNS.stream()
            .map(Column::toString)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        final String CONSTRAINTS_SQL = COLUMNS.stream()
            .map(Column::constraintSQL)
            .filter(c -> c != null)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        final String CHECK_CONSTRAINTS_SQL = CHECK_CONSTRAINTS.stream()
            .map(Expression::toString)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        /* Build the final query */
        StringBuilder sb = new StringBuilder(COLUMNS_SQL);
        if (!COLUMNS_SQL.isEmpty() && !CONSTRAINTS_SQL.isEmpty()) sb.append(", ");
        sb.append(CONSTRAINTS_SQL);
        if (!CHECK_CONSTRAINTS_SQL.isEmpty()) sb.append(", CHECK (").append(CHECK_CONSTRAINTS_SQL).append(")");
        return sb.toString();
    }

    /**
     * Performs a safe table rebuild to apply constraint changes without data loss.
     * In this method, we're using pure SQL via QueryManager to perform the migration.
     */
    private void migrateTable(String newColumnsAndConstraints) {
        Console.log("Schema mismatch detected for %s. Migrating table...", this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();

        String tempTableName = this.name() + "_migration_backup";

        QueryManager.query(this.DATA_BASE, "PRAGMA foreign_keys=OFF;");
        QueryManager.query(this.DATA_BASE, "BEGIN TRANSACTION;");

        try {
            /* Rename existing table to a backup table */
            String existingColumns = QueryManager.queryString(this.DATA_BASE, "SELECT group_concat(name, ',') FROM pragma_table_info(" + this.escapedName() + ");");
            final Set<String> EXISTING_COLUMNS = existingColumns == null ? Set.of() : Set.of(existingColumns.split(","));
            QueryManager.query(this.DATA_BASE, "ALTER TABLE " + this.name() + " RENAME TO " + tempTableName + ";");

            /* Create the new table with updated constraints/enums */
            QueryManager.query(this.DATA_BASE, "CREATE TABLE " + this.name() + " (" + newColumnsAndConstraints + ");");

            /* Identify common columns between the two definitions */
            Set<String> newCols = getRegisteredColumnNames();
            String commonCols = newCols.stream()
                .filter(EXISTING_COLUMNS::contains)
                .map(QueryonEngine::escapeString)
                .collect(Collectors.joining(", "));

            /* Copy existing rows across */
            if (!commonCols.isEmpty()) {
                QueryManager.query(this.DATA_BASE, "INSERT INTO " + this.name() + " (" + commonCols + ") SELECT " + commonCols + " FROM " + tempTableName + ";");
            }

            /* Drop temporary backup table */
            QueryManager.query(this.DATA_BASE, "DROP TABLE " + tempTableName + ";");
            QueryManager.query(this.DATA_BASE, "COMMIT;");
        } catch (QueryonException e) {
            QueryManager.query(this.DATA_BASE, "ROLLBACK;");
            throw new QueryonException(
                "Cannot migrate table '" + this.name() + "': existing data is incompatible with the new schema. " + "Update or remove invalid rows before retrying the migration.",
                e
            );
        } catch (Exception e) {
            QueryManager.query(this.DATA_BASE, "ROLLBACK;");
            throw new RuntimeException("Failed to migrate table " + this.name(), e);
        } finally {
            QueryManager.query(this.DATA_BASE, "PRAGMA foreign_keys=ON;");
        }
    }
    
    @Override public int hashCode() { return name().hashCode(); }
}