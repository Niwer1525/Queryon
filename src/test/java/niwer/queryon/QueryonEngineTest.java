package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }

    @Test void testIsExpression() {
        assertFalse(QueryonEngine.isExpression("Alice"));
        assertFalse(QueryonEngine.isExpression(30));

        assertTrue(QueryonEngine.isExpression("age + 1"));
        assertTrue(QueryonEngine.isExpression("price * quantity"));
        assertTrue(QueryonEngine.isExpression("price + 2 * quantity"));
        assertTrue(QueryonEngine.isExpression("score - 5"));
        assertTrue(QueryonEngine.isExpression("total / 2"));
        assertTrue(QueryonEngine.isExpression("value % 3"));
        assertTrue(QueryonEngine.isExpression("date + INTERVAL 1 DAY"));
        assertTrue(QueryonEngine.isExpression("name || ' ' || surname"));
    }

    @Test void testEscapeString() {
        final String TABLE_NAME = "users";
        assertEquals("\"users\"", QueryonEngine.escapeString(TABLE_NAME));
    }
}
