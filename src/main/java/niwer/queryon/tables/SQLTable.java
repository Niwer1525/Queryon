package niwer.queryon.tables;

import java.util.LinkedHashSet;
import java.util.Set;

import niwer.queryon.DataBase;
import niwer.queryon.interactions.InteractionManager;

public class SQLTable {

    private final DataBase DATA_BASE;
    private final String NAME;
    private final Set<SQLColumn> COLUMNS = new LinkedHashSet<>();

    protected SQLTable(DataBase db, String name) {
        this.DATA_BASE = db;
        this.NAME = name;
    }
    
    /**
     * Add a column to the table definition (Use Table.createColumn or Table.createTextColumn to create a column)
     * @param column The column to add
     * @return The SQLTable instance for chaining
     */
    public SQLTable addColumn(SQLColumn column) {
        COLUMNS.add(column);
        return this;
    }

    /**
     * Add multiple columns to the table definition (Use Table.createColumn or Table.createTextColumn to create columns)
     * @param columns The columns to add
     * @return The SQLTable instance for chaining
     */
    public SQLTable addColumns(SQLColumn... columns) {
        for (final SQLColumn COLUMN : columns) COLUMNS.add(COLUMN);
        return this;
    }

    /**
     * Execute the SQL command to create the table in the database with the defined columns.
     * This should be called after defining all columns for the table.
     */
    public void execute() { //TODO rewrite this method.
        final StringBuilder QUERY = new StringBuilder("CREATE TABLE IF NOT EXISTS " + NAME + " (");
        for (final SQLColumn COLUMN : COLUMNS) QUERY.append(COLUMN.sql()).append(", ");
        QUERY.setLength(QUERY.length() - 2); // Remove last comma and space
        QUERY.append(")");
        InteractionManager.query(this.DATA_BASE, QUERY.toString());
    }
}
