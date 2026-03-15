package niwer.queryon.interactions;

import java.util.HashMap;
import java.util.Map;

import niwer.queryon.DataBase;
import niwer.queryon.SQLSerializable;

public class SelectionManager {

    private final DataBase DATA_BASE;
    private final String TABLE_NAME;
    private final Map<String, Object> WHERE_CONDITIONS = new HashMap<>();

    private SelectionManager(DataBase db, String tableName) {
        this.DATA_BASE = db;
        this.TABLE_NAME = tableName;
    }

    /**
     * Starts a selection query for the specified table.
     * @param tableName The name of the table to select from
     */
    public static SelectionManager select(DataBase db, String tableName) { return new SelectionManager(db, tableName); }

    public SelectionManager where(String column, Object value) {
        WHERE_CONDITIONS.put(column, value);
        return this;
    }
    
    public Object execute() {
        return 0;
    }

    public <T extends SQLSerializable<T>> T execute(Class<T> serializer) {
        final StringBuilder QUERY = new StringBuilder("SELECT * FROM ").append(TABLE_NAME).append(" WHERE ");

        /* Add columns */
        for (final Map.Entry<String, Object> ENTRY : WHERE_CONDITIONS.entrySet()) {
            QUERY.append(ENTRY.getKey()).append(" = ");
            if (ENTRY.getValue() instanceof String) QUERY.append("'").append(ENTRY.getValue()).append("'");
            else QUERY.append(ENTRY.getValue());
            QUERY.append(" AND ");
        }
        QUERY.setLength(QUERY.length() - 5); // Remove the last " AND "
        QUERY.append(";");


        return (T)InteractionManager.query(this.DATA_BASE, serializer, QUERY.toString(), WHERE_CONDITIONS.values().toArray());
    }
}
