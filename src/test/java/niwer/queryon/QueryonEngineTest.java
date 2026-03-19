package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class QueryonEngineTest {

    @TempDir
    private static File tempDir;

    /**
     * Sets up an empty database with the specified name and registers the TestUserTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance
     */
    public static DataBase setupEmptyDB(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    /**
     * Sets up a database with the specified name and registers the TestUserTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance with the TestUserTable registered
     */
    public static DataBase setupUsersDB(String name) {
        return setupEmptyDB(name).registerTable(TestUserTable.class);
    }

    /**
     * Sets up a database with the specified name and registers both the TestUserTable and TestFoodTable.
     * 
     * @param name the name of the database
     * @return the initialized DataBase instance with both the TestUserTable and TestFoodTable registered
     */
    public static DataBase setupUsersAndFoodDB(String name) {
        return setupEmptyDB(name).registerTable(TestUserTable.class).registerTable(TestFoodTable.class);
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
        assertEquals("1970-01-01 01:00:00", SQL_DATE_TIME);
    }

    @Test void testFormatValues() {
        final String FORMATTED = QueryonEngine.formatValues(false, "Alice", 30, true);
        assertEquals("Alice, 30, true", FORMATTED);

        final String FORMATTED_ESCAPED = QueryonEngine.formatValues(true, "Bob", 20, true);
        assertEquals("'Bob', 20, true", FORMATTED_ESCAPED);

        final String FORMATTED_DATE_ESCAPED = QueryonEngine.formatValues(true, "Bob", new Date(0), true);
        assertEquals("'Bob', '1970-01-01 01:00:00', true", FORMATTED_DATE_ESCAPED);
    }
}
