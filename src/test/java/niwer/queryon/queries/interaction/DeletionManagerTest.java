package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.Expression;

class DeletionManagerTest {

    @Test void testDeletionManager(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);

        final String DELETE = DeletionManager.delete(DB, TestUserTable.class)
            .buildQuery();
        assertEquals("DELETE FROM 'test_table'", DELETE);

        final String DELETE_WHERE = DeletionManager.delete(DB, TestUserTable.class)
            .where(Expression.of("name").like("%A%"))
            .buildQuery();
        assertEquals("DELETE FROM 'test_table' WHERE name LIKE '%A%'", DELETE_WHERE);
    }

    @Test void testExecute(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);

        assertDoesNotThrow(() -> {
            DeletionManager.delete(DB, TestUserTable.class)
                .where(Expression.of("name").like("%A%"))
                .execute();
        });
    }
}
