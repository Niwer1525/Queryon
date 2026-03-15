package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.queries.interaction.InsertionManager;

class InsertionManagerTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testInsertion() {
        final DataBase DB = setupDataBase("testInsertion");

        final int INSERTED_ROWS = InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 1)
            .value("name", "Alice")
            .value("age", 30)
            .execute();

        assertEquals(1, INSERTED_ROWS);

        final TestUser USER = InteractionManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 1);
        assertEquals("Alice", USER.name());
    }

     @Test void testInsertionOrIgnore() {
        final DataBase DB = setupDataBase("testInsertionOrIgnore");

        InsertionManager.insertOrIgnore(DB, TestUserTable.class)
            .value("id", 2)
            .value("name", "Bob")
            .value("age", 25)
            .execute();

        // Try to insert the same user again, which should be ignored
        InsertionManager.insertOrIgnore(DB, TestUserTable.class)
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

    @Test void testMultiRowInsertion() {
        final DataBase DB = setupDataBase("testMultiRowInsertion");

        final Map<String, Object> SECOND_ROW = new LinkedHashMap<>();
        SECOND_ROW.put("id", 11);
        SECOND_ROW.put("name", "Maya");
        SECOND_ROW.put("age", 21);

        final int INSERTED_ROWS = InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 10)
            .value("name", "Liam")
            .value("age", 27)
            .row(SECOND_ROW)
            .execute();

        assertEquals(2, INSERTED_ROWS);

        final int USER_COUNT = InteractionManager.queryInt(DB, """
            SELECT COUNT(*) FROM test_table WHERE id IN (?, ?)
        """, 10, 11);
        assertEquals(2, USER_COUNT);
    }

    @Test void testInsertFromSelect() {
        final DataBase DB = setupDataBase("testInsertFromSelect");

        InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 20)
            .value("name", "Anna")
            .value("age", 30)
            .execute();

        final int INSERTED_ROWS = InsertionManager.insert(DB, TestUserTable.class)
            .fromSelect("SELECT ?, ?, COUNT(*) + 5 FROM test_table WHERE age = ?", 21, "Copied", 30)
            .execute();

        assertEquals(1, INSERTED_ROWS);

        final TestUser USER = InteractionManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 21);

        assertEquals("Copied", USER.name());
        assertEquals(6, USER.age());
    }

    @Test void testConflictUpdate() {
        final DataBase DB = setupDataBase("testConflictUpdate");

        InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 30)
            .value("name", "Initial")
            .value("age", 20)
            .execute();

        final int UPDATED_ROWS = InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 30)
            .value("name", "Updated")
            .value("age", 25)
            .onConflict("id")
            .doUpdateSetExcluded("name")
            .doUpdateSetExcluded("age")
            .execute();

        assertEquals(1, UPDATED_ROWS);

        final TestUser USER = InteractionManager.querySerializable(DB, TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 30);

        assertEquals("Updated", USER.name());
        assertEquals(25, USER.age());
    }

    @Test void testReturning() {
        final DataBase DB = setupDataBase("testReturning");

        final TestUser INSERTED = (TestUser) InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 40)
            .value("name", "ReturnUser")
            .value("age", 31)
            .returningAll()
            .execute(TestUser.class);

        assertEquals("ReturnUser", INSERTED.name());
        assertEquals(31, INSERTED.age());
    }

    //  @Test void testInsertionWithoutIgnore() { //TODO
    //     final DataBase DB = setupDataBase("testInsertionWithoutIgnore");

    //     /* Add a new user */
    //     InsertionManager.insert(DB, TestUserTable.class)
    //         .value("id", 3)
    //         .value("name", "Charlie")
    //         .value("age", 28)
    //         .execute();

    //     /* Try to insert the same user (without ignore), which should throw an exception */
    //     assertThrows(SQLException.class, () -> {
    //         InsertionManager.insert(DB, TestUserTable.class)
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
