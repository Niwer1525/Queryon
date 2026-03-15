package niwer.queryon.queries.interaction;

import java.util.HashMap;
import java.util.Map;

import niwer.queryon.DataBase;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.tables.Table;

public class DeletionManager {

    private final DataBase DATA_BASE;
    private final Table TABLE;
    private final Map<String, Object> WHERE_CONDITIONS = new HashMap<>();

    private DeletionManager(DataBase db, Class<? extends Table> table) {
        this.DATA_BASE = db;
        this.TABLE = db.getTable(table);
    }

    /**
     * Starts a deletion query for the specified table.
     * @param table The table to select from
     */
    public static DeletionManager delete(DataBase db, Class<? extends Table> table) { return new DeletionManager(db, table); }

    public DeletionManager where(String column, Object value) {
        WHERE_CONDITIONS.put(column, value);
        return this;
    }
    
    public void execute() {
        final StringBuilder QUERY = new StringBuilder("DELETE FROM ").append(TABLE.name()).append(" WHERE ");

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
