package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;

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

    @Test void testInsertionManagerSQL() {
        final DataBase DB = setupDataBase("testInsertion");

        final String INSERT = InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .buildQuery();
        assertEquals("INSERT INTO test_table (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28)", INSERT);

        final String INSERT_OR_IGNORE = InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .buildQuery();
        assertEquals("INSERT OR IGNORE INTO test_table (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28)", INSERT_OR_IGNORE);
    }

    @Test void testInsertionInvalidValues() {
        final DataBase DB = setupDataBase("testInsertion");

        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(null, TestUserTable.class, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, null, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insert(DB, TestUserTable.class));
        
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(null, TestUserTable.class, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, null, "id", "name", "age"));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, TestUserTable.class, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertionManager.insertOrIgnore(DB, TestUserTable.class));
    }

    @Test void testInsertionManager() {
        final DataBase DB = setupDataBase("testInsertion");

        InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .row(1, "Alice", 30)
            .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .execute();

        final List<TestUser> USERS = SelectionManager.select(DB, TestUserTable.class).executeList(TestUser.class);
        assertEquals(3, USERS.size());
        assertEquals("Alice", USERS.get(0).name());
        assertEquals("Bob", USERS.get(1).name());
        assertEquals("Carol", USERS.get(2).name());
    }
}