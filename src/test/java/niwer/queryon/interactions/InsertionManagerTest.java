package niwer.queryon.interactions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;

class InsertionManagerTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testInsertion() {
        final DataBase DB = setupDataBase("testInsertion");

        InsertionManager.insert(DB, "test_table")
            .value("id", 1)
            .value("name", "Alice")
            .value("age", 30)
            .execute();

        final TestUser USER = InteractionManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 1);
        assertEquals("Alice", USER.name());
    }

     @Test void testInsertionOrIgnore() {
        final DataBase DB = setupDataBase("testInsertionOrIgnore");

        InsertionManager.insertOrIgnore(DB, "test_table")
            .value("id", 2)
            .value("name", "Bob")
            .value("age", 25)
            .execute();

        // Try to insert the same user again, which should be ignored
        InsertionManager.insertOrIgnore(DB, "test_table")
            .value("id", 2)
            .value("name", "Bob")
            .value("age", 25)
            .execute();

        final TestUser USER = InteractionManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 2);
        assertEquals("Bob", USER.name());

        final int USER_COUNT = InteractionManager.queryInt(DB, """
            SELECT COUNT(*) FROM test_table WHERE id = ?
        """, 2);
        assertEquals(1, USER_COUNT); // Ensure only one entry exists for the user
     }

    //  @Test void testInsertionWithoutIgnore() { //TODO
    //     final DataBase DB = setupDataBase("testInsertionWithoutIgnore");

    //     /* Add a new user */
    //     InsertionManager.insert(DB, "test_table")
    //         .value("id", 3)
    //         .value("name", "Charlie")
    //         .value("age", 28)
    //         .execute();

    //     /* Try to insert the same user (without ignore), which should throw an exception */
    //     assertThrows(SQLException.class, () -> {
    //         InsertionManager.insert(DB, "test_table")
    //             .value("id", 3)
    //             .value("name", "Charlie")
    //             .value("age", 28)
    //             .execute();
    //     });

    //     /* Ensure only one entry exists for the user */
    //     final int USER_COUNT = InteractionManager.queryInt(DB, """
    //         SELECT COUNT(*) FROM test_table WHERE id = ?
    //     """, 3);
    //     assertEquals(1, USER_COUNT); // Ensure only one entry exists for the user
    //  }
}
