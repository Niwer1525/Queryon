package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;

class DeletionManagerTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testDeletion() {
        final DataBase DB = setupDataBase("testDeletion");

        /* Insert a user to delete later */
        InsertionManager.insert(DB, TestUserTable.class)
            .value("id", 1)
            .value("name", "Alice")
            .value("age", 30)
            .execute();

        /* Delete the user */
        DeletionManager.delete(DB, TestUserTable.class)
            .where("id", 1)
            .execute();

        /* Try to select the deleted user */
        final Object SINGLE_USER = InteractionManager.query(DB, TestUserTable.TestUser.class, """
            SELECT * FROM test_table WHERE id = ?
        """, 1);
        assertEquals(null, SINGLE_USER); // Ensure the user was deleted
    }
}
