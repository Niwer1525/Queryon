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
}
