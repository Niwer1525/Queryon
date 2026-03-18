package niwer.queryon.queries.interaction;

import java.util.HashSet;
import java.util.Set;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.Table;

/**
 * Manager for building and executing UPDATE queries with a fluent API.
 * Supports setting column values and WHERE conditions clauses.
 * 
 * @author Niwer
 */
public class UpdateManager extends QueryExecutor {

    private final Set<String> SETS = new HashSet<>();
    private Expression whereCondition = null;

    private UpdateManager(DataBase dataBase, Class<? extends Table> tableClas) {
        super(dataBase, tableClas);
    }

    /**
     * Starts an update query for the specified table and columns.
     * 
     * @param db The database to execute the query on
     * @param table The table to update
     * @return An UpdateManager instance to build and execute the query
     */
    public final static UpdateManager update(DataBase db, Class<? extends Table> table) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (table == null) throw new IllegalArgumentException("Table class cannot be null.");
        return new UpdateManager(db, table);
    }

    public final UpdateManager set(String column, Object value) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        this.SETS.add(column + " = " + QueryonEngine.formatValues(true, value));
        return this;
    }

    /**
     * Adds a WHERE condition to the update query.
     * 
     * @param expression The expression representing the WHERE condition
     * @return The UpdateManager instance for chaining
     * 
     * @see Expression for building expressions representing WHERE conditions
     */
    public final UpdateManager where(Expression expression) {
        this.whereCondition = expression;
        return this;
    }

    @Override
    protected String buildQuery() {
        if (this.SETS.isEmpty()) throw new IllegalStateException("At least one column must be set for an update query.");
        final StringBuilder QUERY = new StringBuilder("UPDATE " + this.TABLE.name() + " SET " + String.join(", ", this.SETS));
        
        /* Where condition */
        if (this.whereCondition != null) QUERY.append(" WHERE ").append(this.whereCondition.toString());
        
        return QUERY.toString();
    }

    /**
     * Executes the query.
     */
    public void execute() {
        QueryManager.query(this.DATA_BASE, this.buildQuery());
    }
}
