package niwer.queryon.queries.interaction;

import java.util.LinkedHashSet;
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

    private final Set<String> SETS = new LinkedHashSet<>();
    private Expression whereCondition = null;

    private UpdateManager(DataBase dataBase, Class<? extends Table> tableClas) { super(dataBase, tableClas); }

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

    /**
     * Adds a column and its new value to the update query.
     * 
     * @param column The name of the column to update
     * @param value The new value for the column, which can be a literal value or an expression (e.g., "age + 1")
     * @return The UpdateManager instance for chaining
     */
    public final UpdateManager set(String column, Object value) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        if(value == null) {
            this.SETS.add(column + " = NULL");
            return this;
        }
        this.SETS.add(column + " = " + QueryonEngine.formatValues(!QueryonEngine.isExpression(value), value));
        return this;
    }

    /**
     * Adds a column and a subquery to the update query.
     * 
     * @param column The name of the column to update
     * @param select The SelectionManager instance representing the subquery to use as the new value for the column
     * @return The UpdateManager instance for chaining
     * 
     * @note The subquery will be wrapped in parentheses in the generated SQL query, so the SelectionManager should be built to generate a valid subquery (e.g., "SELECT MAX(age) FROM users").
     * @note if the select parameter is null, it will be treated as setting the column to NULL.
     */
    public final UpdateManager set(String column, SelectionManager select) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        if(select == null) return set(column, (Object) null);
        this.SETS.add(column + " = (" + select.buildQuery() + ")");
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
        final StringBuilder QUERY = new StringBuilder("UPDATE " + this.TABLE.escapedName() + " SET " + String.join(", ", this.SETS));
        
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
