package niwer.queryon.queries.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.TestUserTable.TestUser;
import niwer.queryon.queries.Expression;
import niwer.queryon.queries.interaction.SelectionManager.EnumOrder;

class SelectionManagerTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testSelectionManagerSQL() {
        final DataBase DB = setupDataBase("testSelection");

        final String SELECT_ALL = SelectionManager.select(DB, TestUserTable.class)
            .buildQuery();
        assertEquals("SELECT * FROM test_table", SELECT_ALL);

        final String SELECT_DISTINCT = SelectionManager.selectDistinct(DB, TestUserTable.class)
            .buildQuery();
        assertEquals("SELECT DISTINCT * FROM test_table", SELECT_DISTINCT);

        final String SELECT_COLUMNS = SelectionManager.select(DB, TestUserTable.class, "id", "name")
            .buildQuery();
        assertEquals("SELECT id, name FROM test_table", SELECT_COLUMNS);

        final String SELECT_COLUMNS_WHERE = SelectionManager.select(DB, TestUserTable.class, "id", "name")
            .where(Expression.of("age").isGreaterThan(25))
            .limit(25)
            .buildQuery();
        assertEquals("SELECT id, name FROM test_table WHERE age > 25 LIMIT 25", SELECT_COLUMNS_WHERE);

        final String SELECT_COLUMNS_ORDER_BY = SelectionManager.select(DB, TestUserTable.class, "id", "name")
            .orderBy("id", EnumOrder.ASC)
            .orderBy("email", EnumOrder.ASC)
            .orderBy("name", EnumOrder.DESC)
            .buildQuery();
        assertEquals("SELECT id, name FROM test_table ORDER BY id ASC, email ASC, name DESC", SELECT_COLUMNS_ORDER_BY);

        final String SELECT_COLUMNS_ORDER_BY_WHERE = SelectionManager.select(DB, TestUserTable.class, "id", "name")
            .where(Expression.of("age").isGreaterThan(25))
            .orderBy("id", EnumOrder.ASC)
            .orderBy("email", EnumOrder.ASC)
            .orderBy("name", EnumOrder.DESC)
            .buildQuery();
        assertEquals("SELECT id, name FROM test_table WHERE age > 25 ORDER BY id ASC, email ASC, name DESC", SELECT_COLUMNS_ORDER_BY_WHERE);
    }

    @Test void testSelectionInvalidValues() {
        final DataBase DB = setupDataBase("testSelection");

       assertThrows(IllegalArgumentException.class, () -> SelectionManager.select(null, TestUserTable.class));
       assertThrows(IllegalArgumentException.class, () -> SelectionManager.select(DB, null));

       assertThrows(IllegalArgumentException.class, () -> SelectionManager.selectDistinct(null, TestUserTable.class));
       assertThrows(IllegalArgumentException.class, () -> SelectionManager.selectDistinct(DB, null));

       assertDoesNotThrow(() -> SelectionManager.select(DB, TestUserTable.class, (String[]) null));
       assertDoesNotThrow(() -> SelectionManager.select(DB, TestUserTable.class, new String[] {}));

       assertDoesNotThrow(() -> SelectionManager.selectDistinct(DB, TestUserTable.class, (String[]) null));
       assertDoesNotThrow(() -> SelectionManager.selectDistinct(DB, TestUserTable.class, new String[] {}));
    }

    @Test void testSelectionManager() {
        final DataBase DB = setupDataBase("testSelection");
        
        InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
            .rows(InsertionManager.of(1, "Alice", 30), InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
            .execute();
        
        final TestUser SINGLE_USER = SelectionManager.select(DB, TestUserTable.class).where(Expression.of("name").isEqualTo("Alice")).executeSerializable(TestUser.class);
        assertEquals("Alice", SINGLE_USER.name());
        
        final List<TestUser> USERS = SelectionManager.select(DB, TestUserTable.class).executeList(TestUser.class);
        assertEquals(3, USERS.size());
        assertEquals("Alice", USERS.get(0).name());
        assertEquals("Bob", USERS.get(1).name());
        assertEquals("Carol", USERS.get(2).name());

        assertTrue(SelectionManager.select(DB, TestUserTable.class, "id").where(Expression.of("id").isEqualTo(1)).executeHasResult());

        assertEquals(3, SelectionManager.select(DB, TestUserTable.class, "id").executeCountResults());

        // final int ID = SelectionManager.select(DB, TestUserTable.class, "id").where(Expression.of("id").isEqualTo(1)).executePrimitive(int.class);
        // assertEquals(1, ID);
    }
}