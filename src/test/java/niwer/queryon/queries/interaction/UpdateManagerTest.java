package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.Expression;

class UpdateManagerTest {

    @Test void testUpdateManagerSQL(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final String ESCAPED_TABLE_NAME = QueryonEngine.escapeString("test_table");

        assertThrows(IllegalStateException.class, () -> UpdateManager.update(DB, TestUserTable.class).buildQuery());

        final String UPDATE = UpdateManager.update(DB, TestUserTable.class)
            .set("name", "Alice")
            .set("age", 30)
            .buildQuery();
        assertEquals("UPDATE " + ESCAPED_TABLE_NAME + " SET name = 'Alice', age = 30", UPDATE);

        final String UPDATE_NULL = UpdateManager.update(DB, TestUserTable.class)
            .set("name", "Alice")
            .set("age", null)
            .buildQuery();
        assertEquals("UPDATE " + ESCAPED_TABLE_NAME + " SET name = 'Alice', age = NULL", UPDATE_NULL);

        final String UPDATE_WHERE = UpdateManager.update(DB, TestUserTable.class)
            .set("name", "Alice")
            .set("age", 30)
            .where(Expression.of("id").isEqualTo(1))
            .buildQuery();
        assertEquals("UPDATE " + ESCAPED_TABLE_NAME + " SET name = 'Alice', age = 30 WHERE id = 1", UPDATE_WHERE);

        final String UPDATE_WHERE_AND_SET_EXPRESSION = UpdateManager.update(DB, TestUserTable.class)
            .set("name", "Alice")
            .set("age", "age + 5 * 2.5")
            .where(Expression.of("id").isEqualTo(1))
            .buildQuery();
        assertEquals("UPDATE " + ESCAPED_TABLE_NAME + " SET name = 'Alice', age = age + 5 * 2.5 WHERE id = 1", UPDATE_WHERE_AND_SET_EXPRESSION);

        final SelectionManager SELECT_DISTINCT = SelectionManager.selectDistinct(DB, TestUserTable.class)
            .where(Expression.of("age").isGreaterThan(25));
        final String UPDATE_SUBQUERY = UpdateManager.update(DB, TestUserTable.class)
            .set("name", "Alice")
            .set("age", SELECT_DISTINCT)
            .where(Expression.of("id").isEqualTo(1))
            .buildQuery();
        assertEquals("UPDATE " + ESCAPED_TABLE_NAME + " SET name = 'Alice', age = (SELECT DISTINCT * FROM " + ESCAPED_TABLE_NAME + " WHERE age > 25) WHERE id = 1", UPDATE_SUBQUERY);
    }

    @Test void testUpdateInvalidValues(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);

       assertThrows(IllegalArgumentException.class, () -> UpdateManager.update(null, TestUserTable.class));
       assertThrows(IllegalArgumentException.class, () -> UpdateManager.update(DB, null));
       assertThrows(IllegalArgumentException.class, () -> UpdateManager.update(DB, TestUserTable.class).set(null, "Alice"));
       assertThrows(IllegalArgumentException.class, () -> UpdateManager.update(DB, TestUserTable.class).set(null, SelectionManager.select(DB, TestUserTable.class)));
       assertDoesNotThrow(() -> UpdateManager.update(DB, TestUserTable.class).set("column", null));
    }

    @Test void testUpdateManager(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        
        InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .execute();
        
        final TestUser SINGLE_USER = SelectionManager.select(DB, TestUserTable.class).where(Expression.of("name").isEqualTo("Alice")).executeSerializable(TestUser.class);
        assertNotNull(SINGLE_USER);
        assertEquals("Alice", SINGLE_USER.name());
        
        UpdateManager.update(DB, TestUserTable.class).set("name", "Bob").where(Expression.of("id").isEqualTo(1)).execute();
        assertEquals("Bob", SelectionManager.select(DB, TestUserTable.class).where(Expression.of("id").isEqualTo(1)).executeSerializable(TestUser.class).name());
    }
}
