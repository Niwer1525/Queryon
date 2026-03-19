package niwer.queryon;
import java.io.File;
import java.util.List;

import niwer.lumen.Console;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;

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
            InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
                .row(1, "Alice", 30)
                .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
                .execute();

            /* Insert test foods */
            InsertionManager.insert(DB, TestFoodTable.class, "id", "name", "calories")
                .row(1, "Apple", 1.5)
                .rows(InsertionManager.of(2, "Banana", 2.0), InsertionManager.of(3, "Cherry", 3.0))
                .execute();
        }

        {
            /* Select one of the inserted users (Name: Alice) */
            final TestUser SINGLE_USER = SelectionManager.select(DB, TestUserTable.class)
                .where(Expression.of("name").isEqualTo("Alice"))
                .executeSerializable(TestUser.class);
            Console.log(SINGLE_USER).container(QueryonEngine.LOGGER).send();

            /* Select all users with age greater than 26 */
            final List<TestUser> USERS = SelectionManager.selectDistinct(DB, TestUserTable.class)
                .where(Expression.of("age").isGreaterThan(26))
                .executeList(TestUser.class);
            Console.log(USERS).container(QueryonEngine.LOGGER).send();
        }

        {
            /* Prepare a selection query to check existance of the new user */
            final SelectionManager SELECT_MANAGER = SelectionManager.select(DB, TestUserTable.class)
                .where(Expression.of("name").isEqualTo("Mia"));

            /* Insert a new user */
            InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
                .row(2515, "Mia", 30)
                .execute();

            Console.log("Does Mia exist? " + SELECT_MANAGER.executeHasResult()).container(QueryonEngine.LOGGER).send();

            /* Delete the new user */
            DeletionManager.delete(DB, TestUserTable.class)
                .where(Expression.of("name").isEqualTo("Mia"))
                .execute();

            Console.log("Does Mia exist after deletion? " + SELECT_MANAGER.executeHasResult()).container(QueryonEngine.LOGGER).send();
        }

        {
            /* Insert a new user */
            InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
                .row(255521, "Lou", 22)
                .execute();

            /* Prepare a selection query to check existance of the new user */
            final TestUser INSERTED_USER = SelectionManager.select(DB, TestUserTable.class)
                .where(Expression.of("name").isEqualTo("Lou")).executeSerializable(TestUser.class);
            Console.log(INSERTED_USER).send();
        
            /* Update the new user's name to Louis */
            UpdateManager.update(DB, TestUserTable.class)
                .set("name", "Louis")
                .where(Expression.of("id").isEqualTo(255521))
                .execute();

            /* Check that the user's name has been updated */
            final TestUser UPDATED_USER = SelectionManager.select(DB, TestUserTable.class)
                .where(Expression.of("id").isEqualTo(255521)).executeSerializable(TestUser.class);
            Console.log(UPDATED_USER).send();
        }
    }
}
