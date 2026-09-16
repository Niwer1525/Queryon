package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.QueryonException;
import niwer.queryon.TestUserTable.TestUser;

class QueryManagerTest {
    public static void addUsers(DataBase DB) throws QueryonException {
        QueryManager.query(DB, """
            INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
        """, 1, "Alice", 30);

        QueryManager.query(DB, """
            INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
        """, 2, "Romain", 20);

        QueryManager.query(DB, """
            INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
        """, 3, "Lou", 20);

        QueryManager.query(DB, """
            INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
        """, 4, "Chloée", 20);
    }

    @Test void testNullParameters(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Object[] EMPTY_PARAMS = new Object[]{};
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(null, null, "SELECT * FROM test_table", EMPTY_PARAMS));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, null, null, EMPTY_PARAMS));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, null, "", EMPTY_PARAMS));
    }

    @Test void testNoResultQueryNullParameters(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(null, "INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, (String)null, "INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, "", "", 1, "Alice", 30));
    }

    @Test void testNoResultQuery(@TempDir File tempDir) throws QueryonException {
        assertDoesNotThrow(() -> {
            final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
            QueryManager.query(DB, """
                INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
            """, 0, "Vanessa", 35);
        });
    }

    @Test void testPerQueryConnectionMode(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir).setConnectionMode(DataBase.ConnectionMode.PER_QUERY);

        assertThrows(IllegalArgumentException.class, () -> QueryonEngineTest.setupUsersDB(tempDir).setConnectionMode(null));

        addUsers(DB);

        final Object RESULT = QueryManager.queryInt(DB, """
            SELECT COUNT(*) FROM test_table
        """);
        assertNotNull(RESULT);
        assertInstanceOf(Integer.class, RESULT);
        assertFalse(DB.isConnected(), "Per-query mode should not keep a shared connection open");

        final DataBase DB_2 = QueryonEngineTest.setupUsersDB(tempDir).setConnectionMode(DataBase.ConnectionMode.PERSISTENT);

        addUsers(DB_2);

        final Object RESULT_2 = QueryManager.queryInt(DB_2, """
            SELECT COUNT(*) FROM test_table
        """);
        assertNotNull(RESULT_2);
        assertInstanceOf(Integer.class, RESULT_2);
        assertFalse(DB.isConnected(), "Per-query mode should not keep a shared connection open");
    }

    @Test void testSingleResultSerializableQuery(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object RESULT = QueryManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(RESULT);
        assertInstanceOf(TestUser.class, RESULT);
    }

    @Test void testSingleResultQuery(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object RESULT = QueryManager.query(DB, TestUser.class, """
            SELECT * FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(RESULT);
        assertInstanceOf(TestUser.class, RESULT);
    }

    @Test void testMultipleResultQuery(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object RESULT = QueryManager.queryList(DB, TestUser.class, """
            SELECT * FROM test_table
        """);
        assertNotNull(RESULT);
        assertInstanceOf(List.class, RESULT);
    }

    @Test void testSingleResultQueryList(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        assertDoesNotThrow(() -> {
            QueryManager.queryList(DB, TestUser.class, """
                SELECT * FROM test_table WHERE name = ?
            """, "Alice");
        });
        final List<?> list = (List<?>) QueryManager.queryList(DB, TestUser.class, "SELECT * FROM test_table WHERE name = ?", "Alice");
        assertEquals(1, list.size(), "Expected exactly one result for the query");
    }

    @Test void testExecuteSQLCommandForPrimitive(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object RESULT = QueryManager.queryPrimitive(DB, Integer.class, """
            SELECT COUNT(*) FROM test_table
        """);
        assertNotNull(RESULT);
        assertInstanceOf(Integer.class, RESULT);
    }

    @Test void testQueryPrimitiveWithNoResult(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object RESULT = QueryManager.queryPrimitive(DB, Integer.class, """
            SELECT COUNT(*) FROM test_table WHERE name = ?
        """, "NonExistingName");
        assertNotNull(RESULT);
        assertInstanceOf(Integer.class, RESULT);
        assert(RESULT.equals(0));
    }

    @Test void testPrimitiveFunctions(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final Object COUNT_RESULT = QueryManager.queryInt(DB, """
            SELECT COUNT(*) FROM test_table
        """);
        assertNotNull(COUNT_RESULT);
        assertInstanceOf(Integer.class, COUNT_RESULT);

        final Object STRING_RESULT = QueryManager.queryString(DB, """
            SELECT name FROM test_table WHERE id = ?
        """, 1);
        assertNotNull(STRING_RESULT);
        assertInstanceOf(String.class, STRING_RESULT);

        final Object BOOLEAN_RESULT = QueryManager.queryBoolean(DB, """
            SELECT age > ? FROM test_table WHERE name = ?
        """, 25, "Alice");
        assertNotNull(BOOLEAN_RESULT);
        assertInstanceOf(Boolean.class, BOOLEAN_RESULT);

        final Object DOUBLE_RESULT = QueryManager.queryDouble(DB, """
            SELECT age + 0.5 FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(DOUBLE_RESULT);
        assertInstanceOf(Double.class, DOUBLE_RESULT);

        final Object LONG_RESULT = QueryManager.queryLong(DB, """
            SELECT age + 1 FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(LONG_RESULT);
        assertInstanceOf(Long.class, LONG_RESULT);

        final Object FLOAT_RESULT = QueryManager.queryFloat(DB, """
            SELECT age + 0.5 FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(FLOAT_RESULT);
        assertInstanceOf(Float.class, FLOAT_RESULT);
    }

    @Test void testQueryMap(@TempDir File tempDir) throws QueryonException {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        addUsers(DB);

        final List<Map<String, Object>>  RESULT = QueryManager.queryMap(DB, """
            SELECT * FROM test_table WHERE name = ?
        """, "Alice");
        
        assertNotNull(RESULT);
        assertInstanceOf(List.class, RESULT);
        RESULT.forEach(map -> {
            assertInstanceOf(String.class, map.get("name"));
            assertInstanceOf(Integer.class, map.get("age"));
        });
    }
}
