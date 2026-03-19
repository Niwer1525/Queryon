package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.Expression;

class DeletionManagerTest {

    @Test void testDeletionManager() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("testSelection");

        final String DELETE = DeletionManager.delete(DB, TestUserTable.class)
            .buildQuery();
        assertEquals("DELETE FROM test_table", DELETE);

        final String DELETE_WHERE = DeletionManager.delete(DB, TestUserTable.class)
            .where(Expression.of("name").like("%A%"))
            .buildQuery();
        assertEquals("DELETE FROM test_table WHERE name LIKE '%A%'", DELETE_WHERE);
    }

    @Test void testExecute() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("testSelection");

        assertDoesNotThrow(() -> {
            DeletionManager.delete(DB, TestUserTable.class)
                .where(Expression.of("name").like("%A%"))
                .execute();
        });
    }
}
