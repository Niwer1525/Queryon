package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.Expression;

class TableTest {
    
    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class);
        return DB;
    }

    @Test void testCreateTable() {
        assertDoesNotThrow(() -> new Table(setupDataBase("testCreateTable")) {
            @Override public String name() { return "test_table"; }
        });
    }

    @Test void testCreateEmptyTable() {
        final Table TABLE = new Table(setupDataBase("testCreateTable")) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.execute());
    }

    @Test void testCreateTableWithColumnsAndConstraints() {
        final DataBase DB = setupDataBase("testCreateTableWithColumnsAndConstraints");
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE
            .addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT))
            .addCheckConstraint(Expression.of("int_column").isEqualTo(25))
            .execute()
        );
    }
    
    @Test void testCreateTableIllegalArgs() {
        assertThrows(IllegalArgumentException.class, () -> new Table(null) {
            @Override public String name() { return "test_table"; }

        }, "createTable should throw IllegalArgumentException if DataBase instance is null");

        assertThrows(IllegalArgumentException.class, () -> new Table(setupDataBase("testCreateTableIllegalArgs")) {
            @Override public String name() { return ""; }
        }, "createTable should throw IllegalArgumentException if table name is empty");

        assertThrows(IllegalArgumentException.class, () -> {
            new Table(setupDataBase("testCreateTableIllegalArgsNullName")) {
                @Override public String name() { return null; }
            };
        }, "createTable should throw IllegalArgumentException if table name is null");
    }

    @Test void testCreateColumn() {
        final DataBase DB = setupDataBase("testCreateColumn");
        final Column INT_COLUMN = Table.createColumn(DB, "int_column", EnumColumnTypes.INT);
        assertNotNull(INT_COLUMN, "createColumn should return a non-null SQLColumn instance");

        final Column VARCHAR_COLUMN = Table.createColumn(DB, "varchar_column", 255);
        assertNotNull(VARCHAR_COLUMN, "createTextColumn should return a non-null SQLColumn instance");

        final Column ENUM_COLUMN = Table.createColumn(DB, "enum_column", TestEnum.class);
        assertNotNull(ENUM_COLUMN, "createColumn should return a non-null SQLColumn instance for ENUM type");
    }

    @Test void testAddColumn() {
        final DataBase DB = setupDataBase("testAddColumn");
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        TABLE.addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT));
        assertNotNull(TABLE.columns(), "addColumn should add the column to the table's column map");
        assertEquals(1, TABLE.columns().size(), "addColumn should add the column to the table's column map");
    }

    @Test void testDropTable() {
        final DataBase DB = setupDataBase("testDropTable");
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.dropTable(DB), "dropTable should not throw an exception when dropping an existing table");
    }

    @Test void testDropAllRows() {
        final DataBase DB = setupDataBase("testDropAllRows");
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE.dropAllRows(DB), "dropAllRows should not throw an exception when dropping all rows from an existing table");
    }

    @Test void testCreateColumnIllegalArgs() {
        final DataBase DB = setupDataBase("testCreateColumnIllegalArgs");
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

    private enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }
}
