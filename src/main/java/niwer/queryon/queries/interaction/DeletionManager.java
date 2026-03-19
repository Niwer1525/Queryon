package niwer.queryon.queries.interaction;

import niwer.queryon.DataBase;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.Table;

public class DeletionManager extends QueryExecutor {

    private Expression whereCondition;

    private DeletionManager(DataBase db, Class<? extends Table> table) { super(db, table); }

    /**
     * Starts a deletion query for the specified table.
     * @param table The table to select from
     */
    public static DeletionManager delete(DataBase db, Class<? extends Table> table) { return new DeletionManager(db, table); }

    /**
     * Adds a WHERE condition to the deletion query.
     * 
     * @param expression The expression representing the WHERE condition
     * @return The DeletionManager instance for chaining
     * 
     * @see Expression for building expressions representing WHERE conditions
     */
    public final DeletionManager where(Expression expression) {
        this.whereCondition = expression;
        return this;
    }
    
    @Override
    protected String buildQuery() {
        final StringBuilder QUERY = new StringBuilder("DELETE FROM ").append(TABLE.escapedName());

        /* Add WHERE condition */
        if (this.whereCondition != null) QUERY.append(" WHERE ").append(this.whereCondition);

        return QUERY.toString();
    }

    /**
     * Executes the deletion query on the database.
     */
    public void execute() {
        QueryManager.query(this.DATA_BASE, this.buildQuery());
    }
}
