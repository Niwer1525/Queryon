package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.tables.Table;

class DataBaseTest {

    @TempDir
    private static File tempDir;

    @Test void testConstructor() {
        final File dbFile = new File(tempDir, "test.db");
        assertDoesNotThrow(() -> new DataBase(dbFile));
        assertThrows(IllegalArgumentException.class, () -> new DataBase(null));
    }

    @Test void testConnection() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        try {
            DB.connect();
            assertNotNull(DB.sqlConnection(), "Connection should be established");
    
            DB.disconnect();
            assertNull(DB.sqlConnection(), "Connection should be null after disconnect");
        } finally {
            DB.disconnect();
        }
    }

    @Test void testReconnect() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        try {
            assertTrue(DB.reconnect(), "Reconnect should establish connection when sqlConnection is null");
            assertNotNull(DB.sqlConnection(), "Connection should be established after reconnect");
    
            assertFalse(DB.reconnect(), "Reconnect should return false when connection is already established");
        } finally {
            DB.disconnect();
        }
    }

    @Test void testRegisterTable() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        try {
            DB.connect();
            assertNotNull(DB.sqlConnection(), "Connection should be established");

            DB.registerTable(TestUserTable.class);
        } finally {
            DB.disconnect();
        }
    }

    @Test void testRegisterAlreadyRegisteredTable() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        try {
            DB.connect();
            assertNotNull(DB.sqlConnection(), "Connection should be established");

            DB.registerTable(TestUserTable.class);
            assertThrows(IllegalArgumentException.class, () -> DB.registerTable(TestUserTable.class), "Registering the same table twice should throw an exception");
        } finally {
            DB.disconnect();
        }
    }

    @Test void testGetTable() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"))
            .registerTable(TestUserTable.class)
        ;
        final Table TABLE = DB.getTable(TestUserTable.class);
        assertNotNull(TABLE, "getTable should return the registered table instance");
    }

    @Test void testGetUnregisteredTable() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        assertThrows(IllegalArgumentException.class, () -> DB.getTable(TestUserTable.class), "Getting an unregistered table should throw an exception");
    }

    @Test void testTabExists() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"))
            .registerTable(TestUserTable.class)
        ;
        assertTrue(DB.tabExists(TestUserTable.class), "tabExists should return true for existing table");
        
        Table t = new Table(DB) {
            @Override public String name() { return "non_existent_table"; }
        };
        assertFalse(DB.tabExists(t), "tabExists should return false for non-existent table");
    }

    @Test void testDropTable() {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"))
            .registerTable(TestUserTable.class)
        ;
        assertTrue(DB.tabExists(TestUserTable.class), "Table should exist after registration");

        DB.dropTable(TestUserTable.class);
        assertThrows(IllegalArgumentException.class, () -> DB.getTable(TestUserTable.class), "Table should not exist after dropping");
    }
}
