package niwer.queryon;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import niwer.lumen.Console;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.tables.Table;

/**
 * Represent a Database on which you can register tables and perform SQL operations.
 * You can connect and disconnect from the database, and it will automatically initialize registered tables when connecting.
 * You can also reconnect to refresh the connection if needed.
 * 
 * @author Niwer
 */
public class DataBase {

    private final String DATA_BASE_PATH;
    private final Set<Table> REGISTERED_TABLES = new HashSet<>();
    private Connection sqlConnection = null;
    
    /**
     * Create a new DataBase
     * @param databaseFile The file path to the SQLite database
     */
    public DataBase(File databaseFile) {
        if (databaseFile == null) throw new IllegalArgumentException("Database file cannot be null");
        if (!databaseFile.getParentFile().exists() && !databaseFile.getParentFile().mkdirs())
            Console.log("Failed to create database directory: " + databaseFile.getParent()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        this.DATA_BASE_PATH = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    /**
     * @return The current SQL connection to the database, or null if not connected. Note that this connection may be closed or invalid, so it's recommended to call reconnect() before using it to ensure it's valid.
     */
    public Connection sqlConnection() {
        return sqlConnection;
    }

    /**
     * Reconnect to database if connection is null.
     * 
     * @return true if reconnection was needed, false otherwise
     */
    public boolean reconnect() {
        if (this.sqlConnection == null) {
            connect();
            return this.sqlConnection != null;
        }
        return false;
    }

    /**
     * Safely disconnect from the database, closing the connection if it exists.
     * 
     * @return this Database instance for chaining
     */
    public DataBase disconnect() {
        try {
            if (this.sqlConnection != null) this.sqlConnection.close();
        } catch (SQLException e) {
            Console.log("Failed to disconnect from database: " + e.getMessage()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        } finally {
            this.sqlConnection = null;
        }
        return this;
    }

    /**
     * Establish connection to SQLite database and initialize tables.
     * 
     * @return this Database instance for chaining
     */
    public DataBase connect() {
        try {
            this.sqlConnection = DriverManager.getConnection(this.DATA_BASE_PATH);
        } catch (SQLException e) {
            Console.log("Failed to connect to database: " + e.getMessage()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        }
        return this;
    }

    /**
     * Register a new table to the database. It will initialize it immediately if the database is already <code>connected</code>, otherwise it will be initialized when the database connects.
     * 
     * @param table The table class to register. It must extend the <code>Table</code> class and implement the <code>register()</code> method to define its schema and initialization logic.
     * @return this Database instance for chaining
     */
    public DataBase registerTable(Class<? extends Table> table) {
        this.reconnect(); // Ensure the connection is active before executing the query

        /* Ensure the table is not already registered */
        if(REGISTERED_TABLES.stream().anyMatch(t -> t.getClass().equals(table)))
            throw new IllegalArgumentException("Table " + table.getSimpleName() + " is already registered");

        /* Create an instance of the table and register it */
        try {
            final Table TABLE_INSTANCE = table.getDeclaredConstructor(DataBase.class).newInstance(this);
            REGISTERED_TABLES.add(TABLE_INSTANCE);
            Console.log("Registering SQL table : " + TABLE_INSTANCE.name()).type(QueryonLogTypes.SQL).send();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            Console.log(e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        } finally {
            this.disconnect(); // Ensure the connection is closed after the operation
        }

        return this;
    }

    /**
     * Get a registered table instance by its class. This is used internally by interaction managers to perform operations on the correct table.
     * 
     * @param tableClass The class of the table to retrieve
     * @return The registered table instance corresponding to the specified class
     */
    public Table getTable(Class<? extends Table> tableClass) {
        return REGISTERED_TABLES.stream()
            .filter(t -> t.getClass().equals(tableClass))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Table " + tableClass.getSimpleName() + " is not registered"));
    }

    /**
     * Check if a table is registered in the database by its class.
     * 
     * @param db The DataBase instance to check for the table's existence
     * @param table The table to check
     * @return true if the table is registered in the database, false otherwise
     */
    public boolean tabExists(Table table) {
        this.reconnect(); // Ensure the connection is active before executing the query
        try (final var STATEMENT = this.sqlConnection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='" + table.name() + "';")) {
            return STATEMENT.executeQuery().next(); // If the query returns a result, the table exists
        } catch (SQLException e) {
            Console.log("Failed to check if table exists: " + e.getMessage()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        } finally {
            this.disconnect(); // Ensure the connection is closed after the operation
        }
        return false;
    }

    /**
     * Check if a table is registered in the database by its class.
     * 
     * @param tableClass The class of the table to check
     * @return true if the table is registered in the database, false otherwise
     */
    public boolean tabExists(Class<? extends Table> tableClass) {
        return this.tabExists(this.getTable(tableClass));
    }

    /**
     * Drop a table in the database.
     * This will delete the table and all its data from the database, but it will unregister the table from the DataBase instance.
     * 
     * @param tableClass The class of the table to drop
     * @return The Table instance for chaining
     */
    protected Table dropTable(Class<? extends Table> tableClass) {
        final Table table = this.getTable(tableClass);
        Console.log("Unregistering and dropping table " + table.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        InteractionManager.query(this, "DROP TABLE IF EXISTS " + table.name() + ";");
        REGISTERED_TABLES.remove(table);
        return table;
    }
}
