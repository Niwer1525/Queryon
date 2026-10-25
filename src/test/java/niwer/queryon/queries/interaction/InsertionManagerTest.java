package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.SQLSerializable;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.Expressions;

class InsertionManagerTest {

    private static void assertQueryEquals(String expectedSql, Object[] expectedParams, QueryExecutor executor) {
        assertEquals(expectedSql, executor.buildQuery(), "The generated SQL query does not match the expected SQL query");
        assertArrayEquals(expectedParams, executor.parameters(), "Stored parameters do not match the expected parameters");
    }

    @Test void testInsertionManagerSQL(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final String ESCAPED_TABLE_NAME = QueryonEngine.escapeString("user_table");

        final InsertionManager insertManager = InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28));
        
        assertQueryEquals(
            "INSERT INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)", 
            new Object[]{1, "Alice", 30, 2, "Bob", 25, 3, "Carol", 28}, 
            insertManager
        );

        final InsertionManager insertOrIgnoreManager = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28));
        
        assertQueryEquals(
            "INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)", 
            new Object[]{1, "Alice", 30, 2, "Bob", 25, 3, "Carol", 28}, 
            insertOrIgnoreManager
        );

        final InsertionManager insertDoNothingManager = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoNothing();
            
        assertQueryEquals(
            "INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (?, ?, ?) ON CONFLICT DO NOTHING", 
            new Object[]{1, "Alice", 30}, 
            insertDoNothingManager
        );

        final InsertionManager insertDoUpdateManager = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoUpdate(
                UpdateManager.update(DB, TestUserTable.class)
                             .set("name", "Alice Updated")
                             .where(Expressions.isEqualTo("id", 1))
            );
            
        assertQueryEquals(
            "INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (?, ?, ?) ON CONFLICT DO UPDATE SET name = ? WHERE id = ?", 
            new Object[]{1, "Alice", 30, "Alice Updated", 1}, 
            insertDoUpdateManager
        );

        final TestUser USER_OBJECT = new TestUser(211255, "Romain", 15);
        final InsertionManager insertFromObjectManager = InsertionManager.insert(DB, TestUserTable.class, USER_OBJECT);
        
        assertQueryEquals(
            "INSERT INTO " + ESCAPED_TABLE_NAME + " (id, name, age, createdAt) VALUES (?, ?, ?, ?)", 
            new Object[]{211255, "Romain", 15, QueryonEngine.dateToSQL(LocalDate.now())}, 
            insertFromObjectManager
        );
    }

    @Test void testInsertWithSerializable(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final String ESCAPED_TABLE_NAME = QueryonEngine.escapeString("user_table");

        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class, (SQLSerializable<?>) null).buildQuery());
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, TestUserTable.class, (SQLSerializable<?>) null).buildQuery());

        final TestUser USER_OBJECT = new TestUser(211255, "Romain", 15);
        final InsertionManager insertOrIgnoreFromObject = InsertionManager.insertOrIgnore(DB, TestUserTable.class, USER_OBJECT);
        
        assertQueryEquals(
            "INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age, createdAt) VALUES (?, ?, ?, ?)", 
            new Object[]{211255, "Romain", 15, QueryonEngine.dateToSQL(LocalDate.now())}, 
            insertOrIgnoreFromObject
        );
    }

    @Test void testOnConflictDoNothing(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final String ESCAPED_TABLE_NAME = QueryonEngine.escapeString("user_table");

        final InsertionManager insertDoNothing = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoNothing();
            
        assertQueryEquals(
            "INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (?, ?, ?) ON CONFLICT DO NOTHING", 
            new Object[]{1, "Alice", 30}, 
            insertDoNothing
        );
    }

    @Test void testOnConflictDoUpdateWithNullUpdateManager(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoUpdate(null));
    }

    @Test void testInsertionInvalidValues(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);

        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(null, TestUserTable.class, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, null, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class));
        
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(null, TestUserTable.class, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, null, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, TestUserTable.class, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, TestUserTable.class));
    }

    @Test void testInsertionManager(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);

        InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .execute();

        final List<TestUser> USERS = SelectionManager.select(DB, TestUserTable.class).executeList(TestUser.class);
        assertEquals(3, USERS.size());
        assertEquals("Alice", USERS.get(0).name());
        assertEquals("Bob", USERS.get(1).name());
        assertEquals("Carol", USERS.get(2).name());
    }
}