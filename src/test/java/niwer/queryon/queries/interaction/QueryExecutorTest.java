package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;

public class QueryExecutorTest {

    @Test void testToString(@TempDir File tempDir) {
        QueryExecutor executor = new QueryExecutor(QueryonEngineTest.setupUsersAndFoodDB(tempDir), TestUserTable.class) {
            @Override
            protected String buildQuery() {
                return "SELECT * FROM TestUserTable";
            }
        };

        String expected = executor.getClass().getName() + "@" + Integer.toHexString(executor.hashCode()) + " : SELECT * FROM TestUserTable";
        assertEquals(expected, executor.toString());
    }
}
