package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.queries.interaction.InsertionManager;

class SelectionManagerTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testSelection() {
        final DataBase DB = setupDataBase("testSelection");

        /* Insert a user to select later */
        InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 1)
            .value("name", "Alice")
            .value("age", 30)
            .execute();

        /* Try to select the inserted user */
        final Object SINGLE_USER = InteractionManager.query(DB, TestUserTable.TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 1);
        assertEquals("Alice", ((TestUserTable.TestUser) SINGLE_USER).name()); // Ensure the correct user was selected
    }
}
