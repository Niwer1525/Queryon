package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable.TestUser;

class QueryManagerTest {
    public static void addUsers(DataBase DB) {
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

    @Test void testNullParameters() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("null_parameters");
        final Object[] EMPTY_PARAMS = new Object[]{};
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(null, null, "SELECT * FROM test_table", EMPTY_PARAMS));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, null, null, EMPTY_PARAMS));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, null, "", EMPTY_PARAMS));
    }

    @Test void testNoResultQueryNullParameters() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("no_result_null_parameters");
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(null, "INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, (String)null, "INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30));
        assertThrows(IllegalArgumentException.class, () -> QueryManager.query(DB, "", "", 1, "Alice", 30));
    }

    @Test void testNoResultQuery() {
        assertDoesNotThrow(() -> {
            final DataBase DB = QueryonEngineTest.setupUsersDB("no_result");
            QueryManager.query(DB, """
                INSERT INTO test_table (id, name, age) VALUES (?, ?, ?)
            """, 0, "Vanessa", 35);
        });
    }

    @Test void testSingleResultSerializableQuery() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("single_result_serializable");
        addUsers(DB);

        final Object RESULT = QueryManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(RESULT);
        assertInstanceOf(TestUser.class, RESULT);
    }

    @Test void testSingleResultQuery() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("single_result");
        addUsers(DB);

        final Object RESULT = QueryManager.query(DB, TestUser.class, """
            SELECT * FROM test_table WHERE name = ?
        """, "Alice");
        assertNotNull(RESULT);
        assertInstanceOf(TestUser.class, RESULT);
    }

    @Test void testMultipleResultQuery() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("multiple_result");
        addUsers(DB);

        final Object RESULT = QueryManager.queryList(DB, TestUser.class, """
            SELECT * FROM test_table
        """);
        assertNotNull(RESULT);
        assertInstanceOf(List.class, RESULT);
    }

    @Test void testSingleResultQueryList() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("single_result_query_list");
        addUsers(DB);

        assertThrows(IllegalStateException.class, () -> {
            QueryManager.queryList(DB, TestUser.class, """
                SELECT * FROM test_table WHERE name = ?
            """, "Alice");
        });
    }

    @Test void testExecuteSQLCommandForPrimitive() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("primitive_result");
        addUsers(DB);

        final Object RESULT = QueryManager.queryPrimitive(DB, Integer.class, """
            SELECT COUNT(*) FROM test_table
        """);
        assertNotNull(RESULT);
        assertInstanceOf(Integer.class, RESULT);
    }

    @Test void testQueryPrimitiveWithNoResult() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("primitive_no_result");
        addUsers(DB);

        final Object RESULT = QueryManager.queryPrimitive(DB, Integer.class, """
            SELECT COUNT(*) FROM test_table WHERE name = ?
        """, "NonExistingName");
        assertNotNull(RESULT);
        assertInstanceOf(Integer.class, RESULT);
        assert(RESULT.equals(0));
    }

    @Test void testPrimitiveFunctions() {
        final DataBase DB = QueryonEngineTest.setupUsersDB("primitive_functions");
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
}
