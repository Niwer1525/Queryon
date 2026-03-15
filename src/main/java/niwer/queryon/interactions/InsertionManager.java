package niwer.queryon.interactions;

import java.util.HashMap;
import java.util.Map;

import niwer.queryon.DataBase;

public class InsertionManager {

    private final DataBase DATA_BASE;
    private final String TABLE_NAME;
    private final boolean IGNORE_IF_EXISTS;
    private final Map<String, Object> VALUES = new HashMap<>();

    private InsertionManager(DataBase db, String tableName, boolean ignoreIfExists) {
        this.DATA_BASE = db;
        this.TABLE_NAME = tableName;
        this.IGNORE_IF_EXISTS = ignoreIfExists;
    }

    /**
     * Starts an insertion query for the specified table.
     * This function will not ignore existing entries with the same primary key, use insertOrIgnore() for that.
     * @param tableName The name of the table to select from
     */
    public static InsertionManager insert(DataBase db, String tableName) { return new InsertionManager(db, tableName, false); }

    /**
     * Starts an insertion query for the specified table, ignoring existing entries with the same primary key.
     * @param tableName The name of the table to select from
     */
    public static InsertionManager insertOrIgnore(DataBase db, String tableName) { return new InsertionManager(db, tableName, true); }

    public InsertionManager value(String column, Object value) {
        VALUES.put(column, value);
        return this;
    }
    
    public void execute() {
        final StringBuilder QUERY = new StringBuilder("INSERT ");
        if(this.IGNORE_IF_EXISTS) QUERY.append("OR IGNORE ");
        QUERY.append("INTO ").append(TABLE_NAME).append(" (");

        /* Add columns */
        for (final String COLUMN : VALUES.keySet()) QUERY.append(COLUMN).append(", ");
        QUERY.setLength(QUERY.length() - 2); // Remove last comma and space
        QUERY.append(") VALUES (");

        /* Add values */
        for (final Object VALUE : VALUES.values()) {
            if (VALUE instanceof String) QUERY.append("'").append(VALUE).append("', ");
            else QUERY.append(VALUE).append(", ");
        }
        QUERY.setLength(QUERY.length() - 2); // Remove last comma and space
        QUERY.append(")");

        InteractionManager.query(this.DATA_BASE, QUERY.toString());
    }
}