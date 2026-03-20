package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.QueryonEngineTest.TestEnum;
import niwer.queryon.SQLSerializable;
import niwer.queryon.TestFoodTable;
import niwer.queryon.queries.Expression;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IForeignKey;

class TableTest {

    @Test void testCreateTable(@TempDir File tempDir) {
        assertDoesNotThrow(() -> new Table(QueryonEngineTest.setupUsersDB(tempDir)) {
            @Override public String name() { return "test_table"; }
        });
    }

    @Test void testCreateEmptyTable(@TempDir File tempDir) {
        final Table TABLE = new Table(QueryonEngineTest.setupUsersDB(tempDir)) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.execute());
    }

    @Test void testCreateTableWithColumnsAndConstraints(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE
            .addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT))
            .addCheckConstraint(Expression.of("int_column").isEqualTo(25))
            .execute()
        );
    }
    
    @Test void testCreateTableIllegalArgs(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () -> new Table(null) {
            @Override public String name() { return "test_table"; }

        }, "createTable should throw IllegalArgumentException if DataBase instance is null");

        assertThrows(IllegalArgumentException.class, () -> new Table(QueryonEngineTest.setupUsersDB(tempDir)) {
            @Override public String name() { return ""; }
        }, "createTable should throw IllegalArgumentException if table name is empty");

        assertThrows(IllegalArgumentException.class, () -> {
            new Table(QueryonEngineTest.setupUsersDB(tempDir)) {
                @Override public String name() { return null; }
            };
        }, "createTable should throw IllegalArgumentException if table name is null");
    }

    @Test void testCreateColumn(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Column INT_COLUMN = Table.createColumn(DB, "int_column", EnumColumnTypes.INT);
        assertNotNull(INT_COLUMN, "createColumn should return a non-null SQLColumn instance");

        final Column VARCHAR_COLUMN = Table.createColumn(DB, "varchar_column", 255);
        assertNotNull(VARCHAR_COLUMN, "createTextColumn should return a non-null SQLColumn instance");

        final Column ENUM_COLUMN = Table.createColumn(DB, "enum_column", QueryonEngineTest.TestEnum.class);
        assertNotNull(ENUM_COLUMN, "createColumn should return a non-null SQLColumn instance for ENUM type");
    }

    @Test void testAddColumn(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        TABLE.addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT));
        assertNotNull(TABLE.columns(), "addColumn should add the column to the table's column map");
        assertEquals(1, TABLE.columns().size(), "addColumn should add the column to the table's column map");
    }

    @Test void testDropTable(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.dropTable(), "dropTable should not throw an exception when dropping an existing table");
    }

    @Test void testDropAllRows(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.dropAllRows(), "dropAllRows should not throw an exception when dropping all rows from an existing table");
    }

    @Test void testCreateColumnIllegalArgs(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(null, "column_name", EnumColumnTypes.INT), "createColumn should throw IllegalArgumentException if column name is null");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, null, EnumColumnTypes.INT), "createColumn should throw IllegalArgumentException if column name is null");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "", EnumColumnTypes.INT), "createColumn should throw IllegalArgumentException if column name is empty");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "column_name", (EnumColumnTypes)null), "createColumn should throw IllegalArgumentException if column type is null");

        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, null, 255), "createTextColumn should throw IllegalArgumentException if column name is null");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "", 255), "createTextColumn should throw IllegalArgumentException if column name is empty");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "column_name", 0), "createTextColumn should throw IllegalArgumentException if size is less than or equal to 0");
        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "column_name", -1), "createTextColumn should throw IllegalArgumentException if size is less than or equal to 0");

        assertThrows(IllegalArgumentException.class, () -> Table.createColumn(DB, "null", (Class<? extends Enum<?>>)null), "createColumn should throw IllegalArgumentException if column name is null");
    }

    @Test void testCreateColumnFromAnnotationIllegalArgs(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertThrows(IllegalArgumentException.class, () -> TABLE.addColumnsFromClass(null), "Should throw IllegalArgumentException if class is null");
    }

    @Test void testCreateColumnFromAnnotation(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir)
            .registerTable(TestFoodTable.class);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };

        assertDoesNotThrow(() -> {
            TABLE.addColumnsFromClass(TestAnnotatedClass.class).execute();
        }, "Should not throw an exception when creating a column from a valid ColumnField annotation");
    }

    @Test void testCreateColumnFromAnnotationIllegal(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };

        assertThrows(IllegalArgumentException.class, () -> {
            TABLE.addColumnsFromClass(TestAnnotatedClassIllegalOne.class).execute();
        }, "Should throw IllegalArgumentException when creating a column from an invalid ColumnField annotation");

        assertThrows(IllegalArgumentException.class, () -> {
            TABLE.addColumnsFromClass(TestAnnotatedClassIllegalBis.class).execute();
        }, "Should throw IllegalArgumentException when creating a column from an invalid ColumnField annotation");
    }
    
    private static class TestAnnotatedClass extends SQLSerializable<TestAnnotatedClass> {
        @IColumnField(autoIncrement = true, primaryKey = true)
        private int id ;

        @IColumnField(name = "name", charLimit = 255, notNull = true)
        private String name = "default_name";

        @IColumnField(charLimit = 36, unique = true)
        private String uuid = "default_uuid";

        @IColumnField(name = "food_id", charLimit = 20, foreignKey = @IForeignKey(table = TestFoodTable.class, column = "id", onDelete = EnumForeginKeyAction.CASCADE))
        private String foodId;

        @IColumnField()
        private TestEnum testEnum;

        @IColumnField()
        private Date testDate = new Date();
    }

    private static class TestAnnotatedClassIllegalOne extends SQLSerializable<TestAnnotatedClassIllegalOne> {
        @IColumnField(autoIncrement = true)
        private String name;
    }

    private static class TestAnnotatedClassIllegalBis extends SQLSerializable<TestAnnotatedClassIllegalBis> {
        @IColumnField(charLimit = 255)
        private boolean status;
    }
}
