package niwer.queryon;

import java.io.File;
import java.util.List;

import niwer.lumen.Console;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.interactions.InteractionManager;

public class GlobalTest {

    public static void main(String[] args) {
        /* Initialize the test database */
        final File DB_FILE = new File("./tests/test.db");
        if (DB_FILE.exists()) DB_FILE.delete(); // Ensure the database file is clean before initializing

        final DataBase DB = new DataBase(DB_FILE)
            .registerTable(TestUserTable.class);

        {
            /* Insert test users */
            // InteractionManagerTest.addUsers(DB);

            /* Try to select one of the inserted users */
            final Object SINGLE_USER = InteractionManager.query(DB, TestUser.class, """
                SELECT * FROM test_table WHERE name = ?
            """, "Alice");
            Console.log(SINGLE_USER).container(QueryonEngine.LOGGER).send();

            /* Try to select all users */
            final List<TestUser> USERS = InteractionManager.queryList(DB, TestUser.class, """
                SELECT * FROM test_table
            """);
            Console.log(USERS).container(QueryonEngine.LOGGER).send();

            // (int)executeSQLCommandForPrimitive("SELECT COUNT(*) FROM PlayerAccount WHERE LOWER(email) = LOWER(?)", email.trim()) > 0;
        }
    }
}
