package niwer.queryon.interactions;

import java.util.HashMap;
import java.util.Map;

import niwer.queryon.DataBase;

public class DeletionManager {

    private final DataBase DATA_BASE;
    private final String TABLE_NAME;
    private final Map<String, Object> WHERE_CONDITIONS = new HashMap<>();

    private DeletionManager(DataBase db, String tableName) {
        this.DATA_BASE = db;
        this.TABLE_NAME = tableName;
    }

    /**
     * Starts a deletion query for the specified table.
     * @param tableName The name of the table to select from
     */
    public static DeletionManager delete(DataBase db, String tableName) { return new DeletionManager(db, tableName); }

    public DeletionManager where(String column, Object value) {
        WHERE_CONDITIONS.put(column, value);
        return this;
    }
    
    public void execute() {
        final StringBuilder QUERY = new StringBuilder("DELETE FROM ").append(TABLE_NAME).append(" WHERE ");

        /* Add columns */
        for (final Map.Entry<String, Object> ENTRY : WHERE_CONDITIONS.entrySet()) {
            QUERY.append(ENTRY.getKey()).append(" = ");
            if (ENTRY.getValue() instanceof String) QUERY.append("'").append(ENTRY.getValue()).append("'");
            else QUERY.append(ENTRY.getValue());
            QUERY.append(" AND ");
        }
        QUERY.setLength(QUERY.length() - 5); // Remove the last " AND "
        QUERY.append(";");

        InteractionManager.query(this.DATA_BASE, QUERY.toString());
    }
}
