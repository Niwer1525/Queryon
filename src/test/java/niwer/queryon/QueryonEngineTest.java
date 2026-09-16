package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class QueryonEngineTest {

    private static DataBase setupDB(File dir) {
        return new DataBase(new File(dir, UUID.randomUUID() + ".db"));
    }

    /**
     * Sets up an empty database with the specified name and registers the TestUserTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance
     */
    public static DataBase setupEmptyDB(File dir) {
        return setupDB(dir);
    }

    /**
     * Sets up a database with the specified name and registers the TestUserTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance with the TestUserTable registered
     */
    public static DataBase setupUsersDB(File dir) {
        return setupEmptyDB(dir).registerTable(TestUserTable.class);
    }

    /**
     * Sets up a database with the specified name and registers both the TestUserTable and TestFoodTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance with both the TestUserTable and TestFoodTable registered
     */
    public static DataBase setupUsersAndFoodDB(File dir) {
        return setupEmptyDB(dir).registerTable(TestUserTable.class).registerTable(TestFoodTable.class);
    }

    public static enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    @Test void testInstances() {
        assertEquals(QueryonEngine.class, new QueryonEngine().getClass());
        assertEquals(QueryonLogTypes.class, new QueryonLogTypes().getClass());
    }

    @Test void testDateToSQL() {
        final Date DATE = new Date(0); // January 1, 1970
        final String SQL_DATE = QueryonEngine.dateToSQL(DATE);
        assertEquals("1970-01-01", SQL_DATE);
    }

    @Test void testDateTimeToSQL() {
        final Date DATE = new Date(0); // January 1, 1970
        final String SQL_DATE_TIME = QueryonEngine.dateTimeToSQL(DATE);
        assertEquals("1970-01-01 00:00:00", SQL_DATE_TIME);
    }

    @Test void testFormatValues() {
        final String FORMATTED = QueryonEngine.formatValues(false, "Alice", 30, true);
        assertEquals("Alice, 30, true", FORMATTED);

        final String FORMATTED_ESCAPED = QueryonEngine.formatValues(true, "Bob", 20, true);
        assertEquals("'Bob', 20, true", FORMATTED_ESCAPED);

        final String FORMATTED_DATE_ESCAPED = QueryonEngine.formatValues(true, "Bob", new Date(0), true);
        assertEquals("'Bob', '1970-01-01 00:00:00', true", FORMATTED_DATE_ESCAPED);

        final String FORMATTED_ENUM = QueryonEngine.formatValues(true, "Bob", TestEnum.VALUE1, true);
        assertEquals("'Bob', 'VALUE1', true", FORMATTED_ENUM);
    }

    @Test void testRawExpression() {
        assertEquals("age + 1", QueryonEngine.raw("age + 1").toString());

        assertThrows(IllegalArgumentException.class, () -> QueryonEngine.raw(null));
        assertThrows(IllegalArgumentException.class, () -> QueryonEngine.raw(""));
    }

    @Test void testIsExpression() {
        assertFalse(QueryonEngine.isExpression("Alice"));
        assertFalse(QueryonEngine.isExpression(30));
        assertFalse(QueryonEngine.isExpression("2026-03-20T19:59:29"));

        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("age + 1")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("price * quantity")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("price + 2 * quantity")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("score - 5")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("total / 2")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("value % 3")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("date + INTERVAL 1 DAY")));
        assertTrue(QueryonEngine.isExpression(QueryonEngine.raw("name || ' ' || surname")));
    }

    @Test void testEscapeString() {
        final String TABLE_NAME = "users";
        assertEquals("\"users\"", QueryonEngine.escapeString(TABLE_NAME));
    }

    @Test void testWrap() {
        assertThrows(IllegalArgumentException.class, () -> QueryonEngine.wrap(null));

        assertEquals(Void.class, QueryonEngine.wrap(void.class));
        assertEquals(Integer.class, QueryonEngine.wrap(int.class));
        assertEquals(Long.class, QueryonEngine.wrap(long.class));
        assertEquals(Boolean.class, QueryonEngine.wrap(boolean.class));
        assertEquals(Double.class, QueryonEngine.wrap(double.class));
        assertEquals(Float.class, QueryonEngine.wrap(float.class));
        assertEquals(Byte.class, QueryonEngine.wrap(byte.class));
        assertEquals(Short.class, QueryonEngine.wrap(short.class));
        assertEquals(Character.class, QueryonEngine.wrap(char.class));

        assertEquals(Object.class, QueryonEngine.wrap(Object.class));
        assertEquals(String.class, QueryonEngine.wrap(String.class));
    }
}
