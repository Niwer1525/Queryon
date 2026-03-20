package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.Expression;

class InsertionManagerTest {

    @Test void testInsertionManagerSQL(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final String ESCAPED_TABLE_NAME = QueryonEngine.escapeString("test_table");

        final String INSERT = InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .buildQuery();
        assertEquals("INSERT INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28)", INSERT);

        final String INSERT_OR_IGNORE = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .buildQuery();
        assertEquals("INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28)", INSERT_OR_IGNORE);

        final String INSERT_DO_NOTHING = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoNothing()
            .buildQuery();
        assertEquals("INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (1, 'Alice', 30) ON CONFLICT DO NOTHING", INSERT_DO_NOTHING);

        final String INSERT_DO_UPDATE = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .onConflictDoUpdate(
                UpdateManager.update(DB, TestUserTable.class).set("name", "Alice Updated").where(Expression.of("id").isEqualTo(1))
            )
            .buildQuery();
        assertEquals("INSERT OR IGNORE INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (1, 'Alice', 30) ON CONFLICT DO UPDATE SET name = 'Alice Updated' WHERE id = 1", INSERT_DO_UPDATE);

        final TestUser USER_OBJECT = new TestUser(211255, "Romain", 15);
        final String INSERT_FROM_OBJECT = InsertionManager.insert(DB, TestUserTable.class, USER_OBJECT)
            .buildQuery();
        assertEquals("INSERT INTO " + ESCAPED_TABLE_NAME + " (id, name, age) VALUES (211255, 'Romain', 15)", INSERT_FROM_OBJECT);
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