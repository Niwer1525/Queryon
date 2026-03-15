package niwer.queryon.queries.interaction;

import java.util.HashMap;
import java.util.Map;

import niwer.queryon.DataBase;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.BuiltQuery;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.queries.QueryExecutor;
import niwer.queryon.tables.Table;

public class SelectionManager extends QueryExecutor {

    private final Map<String, Object> WHERE_CONDITIONS = new HashMap<>();

    private SelectionManager(DataBase db, Class<? extends Table> table) {
        super(db, table);
    }

    /**
     * Starts a selection query for the specified table.
     * @param table The table to select from
     */
    public static SelectionManager select(DataBase db, Class<? extends Table> table) { return new SelectionManager(db, table); }

    public SelectionManager where(String column, Object value) {
        WHERE_CONDITIONS.put(column, value);
        return this;
    }
    
    public Object execute() {
        return 0;
    }

    /**
     * Executes the selection query and returns the result as an instance of the specified serializer class.
     * 
     * @param <T> The type of the serializer class
     * @param serializer The class of the serializer to use for deserializing the result
     * @return An instance of the specified serializer class containing the result of the query
     */
    public <T extends SQLSerializable<T>> T execute(Class<T> serializer) {
        final BuiltQuery QUERY = buildQuery();
        return InteractionManager.querySerializable(this.DATA_BASE, serializer, QUERY.sql(), QUERY.asArray());
    }

    @Override
    protected BuiltQuery buildQuery() {
        final StringBuilder QUERY = new StringBuilder("SELECT * FROM ").append(TABLE.name()).append(" WHERE ");

        /* Add columns */
        for (final Map.Entry<String, Object> ENTRY : WHERE_CONDITIONS.entrySet()) {
            QUERY.append(ENTRY.getKey()).append(" = ");
            if (ENTRY.getValue() instanceof String) QUERY.append("'").append(ENTRY.getValue()).append("'");
            else QUERY.append(ENTRY.getValue());
            QUERY.append(" AND ");
        }
        QUERY.setLength(QUERY.length() - 5); // Remove the last " AND "
        QUERY.append(";");
        return new BuiltQuery(QUERY.toString(), WHERE_CONDITIONS.values());
    }
}
