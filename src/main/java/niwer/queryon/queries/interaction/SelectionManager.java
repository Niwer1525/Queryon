package niwer.queryon.queries.interaction;

import java.util.LinkedHashSet;
import java.util.Set;

import niwer.queryon.DataBase;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.Expression;
import niwer.queryon.tables.Table;

/**
 * Manager for building and executing SELECT queries.
 * 
 * @author Niwer
 */
public class SelectionManager extends QueryExecutor {

    private final String[] COLUMNS;
    private final Set<Order> ORDER_BY = new LinkedHashSet<>();
    private Expression whereCondition = null;

    private SelectionManager(DataBase db, Class<? extends Table> table, String... columns) {
        super(db, table);
        this.COLUMNS = columns;
    }

    /**
     * Starts a selection query for the specified table.
     * 
     * @param db The database to execute the query on
     * @param table The table to select from
     */
    public final static SelectionManager select(DataBase db, Class<? extends Table> table) { return select(db, table, "*"); }

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
        return new SelectionManager(db, table, columns == null || columns.length == 0 ? new String[] { "*" } : columns);
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
    public final SelectionManager orderBy(String column, Order order) {
        this.ORDER_BY.add(order);
        return this;
    }

    @Override
    protected String buildQuery() {
        // final StringBuilder QUERY = new StringBuilder("SELECT * FROM ").append(TABLE.name()).append(" WHERE ");

        // /* Add columns */
        // for (final Map.Entry<String, Object> ENTRY : WHERE_CONDITIONS.entrySet()) {
        //     QUERY.append(ENTRY.getKey()).append(" = ");
        //     if (ENTRY.getValue() instanceof String) QUERY.append("'").append(ENTRY.getValue()).append("'");
        //     else QUERY.append(ENTRY.getValue());
        //     QUERY.append(" AND ");
        // }
        // QUERY.setLength(QUERY.length() - 5); // Remove the last " AND "
        // QUERY.append(";");
        // return new BuiltQuery(QUERY.toString(), WHERE_CONDITIONS.values());
        return null;
    }

    public final Object execute() {
        return 0;
    }

    /**
     * Executes the selection query and returns the result as an instance of the specified serializer class.
     * 
     * @param <T> The type of the serializer class
     * @param serializer The class of the serializer to use for deserializing the result
     * @return An instance of the specified serializer class containing the result of the query
     */
    public final <T extends SQLSerializable<T>> T execute(Class<T> serializer) {
        // final BuiltQuery QUERY = buildQuery();
        // return InteractionManager.querySerializable(this.DATA_BASE, serializer, QUERY, QUERY.asArray());
        return null;
    }

    public static enum Order { ASC, DESC; }
}
