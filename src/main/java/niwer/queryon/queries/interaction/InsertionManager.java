package niwer.queryon.queries.interaction;

import java.util.ArrayList;
import java.util.List;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.Table;

/**
 * Manager for building and executing INSERT queries with a fluent API.
 * Supports single and multi-row inserts, inserting from SELECT and UPSERT (ON CONFLICT) clauses.
 * 
 * @author Niwer
 * 
 * //TODO add support for ON CONFLICT (SQLite) for upsert functionality
 */
public class InsertionManager extends QueryExecutor {

    private final String[] COLUMNS;
    private final List<Object[]> ROWS = new ArrayList<>();
    private final boolean IGNORE_CONFLICTS;

    private InsertionManager(DataBase db, Class<? extends Table> table, boolean ignore, String... columns) {
        super(db, table);
        this.COLUMNS = columns;
        this.IGNORE_CONFLICTS = ignore;
    }

    /**
     * Starts an insertion query for the specified table and columns.
     * 
     * @param db The database to execute the query on
     * @param table The table to insert into
     * @param columns The columns to insert into (if empty, inserts into all columns)
     * @return An InsertionManager instance to build and execute the query
     */
    public final static InsertionManager insert(DataBase db, Class<? extends Table> table, String... columns) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (table == null) throw new IllegalArgumentException("Table class cannot be null.");
        if (columns == null || columns.length == 0) throw new IllegalArgumentException("At least one column must be specified for insertion.");
        return new InsertionManager(db, table, false, columns);
    }

    /**
     * Starts an insertion query for the specified table and columns, with the option to ignore conflicts.
     * 
     * @param db The database to execute the query on
     * @param table The table to insert into
     * @param columns The columns to insert into (if empty, inserts into all columns)
     * @return An InsertionManager instance to build and execute the query
     */
    public final static InsertionManager insertOrIgnore(DataBase db, Class<? extends Table> table, String... columns) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (table == null) throw new IllegalArgumentException("Table class cannot be null.");
        if (columns == null || columns.length == 0) throw new IllegalArgumentException("At least one column must be specified for insertion.");
        return new InsertionManager(db, table, true, columns);
    }

    /**
     * Adds a row of values to be inserted.
     * Can be called multiple times to insert multiple rows.
      *
     * @param value The values for the row, in the same order as the specified columns
     * @return The InsertionManager instance for chaining
     */
    public final InsertionManager row(Object... value) {
        ROWS.add(value);
        return this;
    }

    /**
     * Adds multiple rows of values to be inserted.
     * 
     * @param values An array of value arrays, where each inner array represents a row of values in the same order as the specified columns
     * @return The InsertionManager instance for chaining
     */
    public final InsertionManager rows(Object[]... values) {
        for (final Object[] VALUE : values) ROWS.add(VALUE);
        return this;
    }

    @Override
    protected String buildQuery() {
        final StringBuilder QUERY = new StringBuilder("INSERT");
        if (IGNORE_CONFLICTS) QUERY.append(" OR IGNORE");
        QUERY.append(" INTO ").append(TABLE.name()).append(" (").append(QueryonEngine.formatValues(COLUMNS)).append(") VALUES ");
        
        /* Add objects */
        final String VALUES_SQL = ROWS.stream()
            .map(row -> "(" + QueryonEngine.formatValues(true, row) + ")")
            .reduce((a, b) -> a + ", " + b)
            .orElseThrow(() -> new IllegalStateException("Failed to build insertion query: No rows to insert."));

        QUERY.append(VALUES_SQL);
        
        return QUERY.toString();
    }

    /**
     * Executes the built insertion query against the database.
     */
    public final void execute() {
        QueryManager.query(this.DATA_BASE, this.buildQuery());
    }

    public static final Object[] of(Object... values) { return values; }
}