package niwer.queryon.tables;

import niwer.queryon.DataBase;

/**
 * Represents a database table that can be registered with a DataBase instance.
 * 
 * @see Table#createTable(String)
 * @see Table#createColumn(String, EnumColumnTypes)
 * @see Table#createTextColumn(String, int)
 */
public abstract class Table {

    private SQLTable sqlTable;

    public abstract void register(DataBase db);

    /**
     * Helper method to create a table definition. It returns an SQLTable instance that can be used to define the columns of the table using SQLTable.addColumn() and then execute the table creation with SQLTable.execute().
     * @param name The name of the table to create in the database
     * @return An SQLTable instance that can be used to define the columns of the table and execute the table creation in the database
     */
    protected SQLTable createTable(DataBase db, String name) {
        if (db == null) throw new IllegalArgumentException("DataBase instance cannot be null.");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Table name cannot be null or empty.");
        if (this.sqlTable != null) throw new IllegalStateException("Table definition already created. A table can only be created once per Table instance.");
        
        return this.sqlTable = new SQLTable(db, name);
    }

    /**
     * Helper method to create a column definition for the table. It supports basic column types (INT, VARCHAR, BOOLEAN) and allows to set various constraints (NOT NULL, UNIQUE, AUTO_INCREMENT, PRIMARY KEY) and default values.
     * @param name The name of the column
     * @param type The type of the column (INT, VARCHAR, BOOLEAN)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected SQLColumn createColumn(String name, EnumColumnTypes type) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        if (type == null) throw new IllegalArgumentException("Column type cannot be null.");

        return new SQLColumn(name, type, 0);
    }

    /**
     * Helper method to create a VARCHAR column definition for the table. It allows to specify the size of the VARCHAR column and supports the same constraints and default values as createColumn.
     * @param name The name of the column
     * @param size The size of the VARCHAR column (maximum number of characters)
     * @return An SQLColumn instance that can be further configured with constraints and default values, and then added to a table definition using SQLTable.addColumn()
     */
    protected SQLColumn createTextColumn(String name, int size) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty.");
        if (size <= 0) throw new IllegalArgumentException("VARCHAR column size must be greater than 0.");
        
        return new SQLColumn(name, EnumColumnTypes.VARCHAR, size);
    }

    public SQLTable sqlTable() { return this.sqlTable; }
}