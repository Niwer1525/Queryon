package niwer.queryon;

import java.io.File;

import niwer.lumen.Console;

/**
 * This is a global test class that demonstrates the usage of the Queryon library.
 * It initializes a test database, registers some tables, inserts test data, and performs some queries to verify that everything is working correctly. This class can be used as a reference for how to use the Queryon library in a real application.
 */
public class GlobalTest {

    public static void main(String[] args) {
        /* Initialize the test database */
        final File DB_FILE = new File("./tests/test.db");
        if (DB_FILE.exists()) DB_FILE.delete(); // Ensure the database file is clean before initializing

        final DataBase DB = new DataBase(DB_FILE)
            .registerTable(TestUserTable.class) // Create and register the TestUserTable, which will automatically create the table in the database
            .registerTable(TestFoodTable.class)
        ;

        {
            /* You can drop a table inside it's class directly or by getting the table via the DataBase instance */
            DB.dropTable(TestUserTable.class); // Delete the TestUserTable if it exists, which will drop the table from the database
            DB.registerTable(TestUserTable.class); // Recreate the TestUserTable after dropping (deleting) it
            final boolean TEST_USER_TABLE_EXIST = DB.tabExists(TestUserTable.class);
            Console.log("User Table exists : " + TEST_USER_TABLE_EXIST).container(QueryonEngine.LOGGER).send();
        }

        {
            /* Insert test users */
            // InsertionManager.insert(DB, TestUserTable.class)
            //     .value("id", 1)
            //     .value("name", "Alice")
            //     .value("age", 30)
            //     .execute();

            // InsertionManager.insert(DB, TestUserTable.class)
            //     .value("id", 2)
            //     .value("name", "Bob")
            //     .value("age", 30)
            //     .execute();
        }

        {
            /* Try to select one of the inserted users */
            // final Object SINGLE_USER = SelectionManager.select(DB, TestUser.class)
            //     .where("name", "Alice")
            //     .execute();
            // Console.log(SINGLE_USER).container(QueryonEngine.LOGGER).send();

            /* Try to select all users */
            // final List<TestUser> USERS = InteractionManager.queryList(DB, TestUser.class, """
            //     SELECT * FROM test_table
            // """);
            // Console.log(USERS).container(QueryonEngine.LOGGER).send();

            // (int)executeSQLCommandForPrimitive("SELECT COUNT(*) FROM PlayerAccount WHERE LOWER(email) = LOWER(?)", email.trim()) > 0;
        }
    }
}
