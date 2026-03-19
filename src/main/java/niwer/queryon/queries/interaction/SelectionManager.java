package niwer.queryon.queries.interaction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.Table;

/**
 * Manager for building and executing SELECT queries.
 * 
 * @author Niwer
 */
public class SelectionManager extends QueryExecutor {

    private final boolean IS_DISTINCT;
    private final String[] COLUMNS;
    private final Map<String, EnumOrder> ORDER_BY = new LinkedHashMap<>();
    private Expression whereCondition = null;
    private int limit = -1;

    private SelectionManager(DataBase db, Class<? extends Table> table, boolean isDistinct, String... columns) {
        super(db, table);
        this.IS_DISTINCT = isDistinct;
        this.COLUMNS = columns;
    }

    /**
     * Starts a selection query for the specified table and columns.
     * 
     * @param db The database to execute the query on
     * @param table The table to select from
     * @param columns The columns to select (if empty, selects all columns)
     * @return A SelectionManager instance to build and execute the query
     */
    public final static SelectionManager select(DataBase db, Class<? extends Table> table, String... columns) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (table == null) throw new IllegalArgumentException("Table class cannot be null.");
        return new SelectionManager(db, table, false, columns == null || columns.length == 0 ? new String[] { "*" } : columns);
    }

    /**
     * Starts a distinct selection query for the specified table and columns.
     * The distinct keyword will ensure that the query result only contain unique rows, which can be useful when you want to avoid duplicates in the result.
     * 
     * @param db The database to execute the query on
     * @param table The table to select from
     * @param columns The columns to select (if empty, selects all columns)
     * @return A SelectionManager instance to build and execute the query
     */
    public final static SelectionManager selectDistinct(DataBase db, Class<? extends Table> table, String... columns) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (table == null) throw new IllegalArgumentException("Table class cannot be null.");
        return new SelectionManager(db, table, true, columns == null || columns.length == 0 ? new String[] { "*" } : columns);
    }

    /**
     * Adds a WHERE condition to the selection query.
     * 
     * @param expression The expression representing the WHERE condition
     * @return The SelectionManager instance for chaining
     * 
     * @see Expression for building expressions representing WHERE conditions
     */
    public final SelectionManager where(Expression expression) {
        this.whereCondition = expression;
        return this;
    }

    /**
     * Adds an ORDER BY clause to the selection query for the specified column and order.
     * 
     * @param column The column to order by
     * @param order The order (ASC or DESC) to sort the results
     * @return The SelectionManager instance for chaining
     */
    public final SelectionManager orderBy(String column, EnumOrder order) {
        this.ORDER_BY.put(column, order);
        return this;
    }

    /**
     * Sets the maximum number of rows to return from the selection query.
     * 
     * @param limit The maximum number of rows to return
     * @return The SelectionManager instance for chaining
     */
    public final SelectionManager limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    protected String buildQuery() {
        final StringBuilder QUERY = new StringBuilder("SELECT ");
        if (IS_DISTINCT) QUERY.append("DISTINCT ");
        QUERY.append(QueryonEngine.formatValues(COLUMNS)).append(" FROM ").append(TABLE.escapedName());

        /* Add where */
        if (whereCondition != null) QUERY.append(" WHERE ").append(whereCondition.toString());

        /* Add order by */
        if (!ORDER_BY.isEmpty()) {
            final String VALUES_SQL = ORDER_BY.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue().name())
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow(() -> new IllegalStateException("Failed to build selection query: No columns specified for ORDER BY clause."));

            QUERY.append(" ORDER BY ").append(VALUES_SQL);
        }

        if (limit >= 0) QUERY.append(" LIMIT ").append(limit);

        return QUERY.toString();
    }

    /**
     * Executes the selection query and returns the result as an instance of the specified serializer class.
     * 
     * @param <T> The type of the serializer class
     * @param serializer The class of the serializer to use for deserializing the result
     * @return An instance of the specified serializer class containing the result of the query
     */
    public final <T extends SQLSerializable<T>> T executeSerializable(Class<T> serializer) {
        return QueryManager.querySerializable(this.DATA_BASE, serializer, this.buildQuery());
    }

    /**
     * Executes the selection query and returns the result as a list of instances of the specified serializer class.
     * 
     * @param <T> The type of the serializer class
     * @param serializer The class of the serializer to use for deserializing the result
     * @return A list of instances of the specified serializer class containing the result of the query
     */
    public final <T extends SQLSerializable<T>> List<T> executeList(Class<T> serializer) {
        return QueryManager.queryList(this.DATA_BASE, serializer, this.buildQuery());
    }

    /**
     * Executes the selection query and returns the result as a primitive value of the specified type.
     * 
     * @param <T> The type of the primitive value
     * @param type The class of the primitive type to return (e.g. Integer.class, String.class, etc.)
     * @return A primitive value of the specified type containing the result of the query
     */
    public final <T> T executePrimitive(Class<T> type) {
        return QueryManager.queryPrimitive(this.DATA_BASE, type, this.buildQuery());
    }

    /**
     * Executes the selection query and returns true if there is at least one result, false otherwise.
     * 
     * @return True if there is at least one result, false otherwise
     */
    public final boolean executeHasResult() {
        return QueryManager.queryHasResult(this.DATA_BASE, this.buildQuery());
    }

    /**
     * Executes the selection query and returns the count of results returned by the query.
     * 
     * @return The count of results returned by the query, or 0 if the query returns no results or if an error occurs
     */
    public final int executeCountResults() {
        return QueryManager.queryCountResults(this.DATA_BASE, this.buildQuery());
    }

    public static enum EnumOrder { ASC, DESC; }
}
