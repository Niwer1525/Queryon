package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import niwer.queryon.DataBase;

class TableTest {

    @Test void testCreateTable() {
        new Table() {
            @Override
            public void register(DataBase dataBase) {
                this.createTable(dataBase, "test_table");
                assertNotNull(this.sqlTable(), "createTable should return a non-null SQLTable instance");
            }
        };
    }

    @Test void testCreateTableIllegalArgs() {
        new Table() {
            @Override
            public void register(DataBase dataBase) {
                assertThrows(IllegalArgumentException.class, () -> createTable(null, "test_table"), "createTable should throw IllegalArgumentException if DataBase instance is null");
                assertThrows(IllegalArgumentException.class, () -> createTable(dataBase, null), "createTable should throw IllegalArgumentException if table name is null");
                assertThrows(IllegalArgumentException.class, () -> createTable(dataBase, ""), "createTable should throw IllegalArgumentException if table name is empty");
            }
        };
    }

    @Test void testCreateTableAlreadyExist() {
        new Table() {
            @Override
            public void register(DataBase dataBase) {
                this.createTable(dataBase, "test_table");
                assertNotNull(this.sqlTable(), "createTable should return a non-null SQLTable instance");

                assertThrows(IllegalStateException.class, () -> createTable(dataBase, "test_table"), "createTable should throw IllegalStateException if called more than once on the same Table instance");
            }
        };
    }

    @Test void testCreateColumn() {
        new Table() {
            @Override
            public void register(DataBase dataBase) {
                final SQLColumn INT_COLUMN = this.createColumn("int_column", EnumColumnTypes.INT);
                assertNotNull(INT_COLUMN, "createColumn should return a non-null SQLColumn instance");

                final SQLColumn VARCHAR_COLUMN = this.createTextColumn("varchar_column", 255);
                assertNotNull(VARCHAR_COLUMN, "createTextColumn should return a non-null SQLColumn instance");
            }
        };
    }

    @Test void testCreateColumnIllegalArgs() {
        new Table() {
            @Override
            public void register(DataBase dataBase) {
                assertThrows(IllegalArgumentException.class, () -> createColumn(null, EnumColumnTypes.INT), "createColumn should throw IllegalArgumentException if column name is null");
                assertThrows(IllegalArgumentException.class, () -> createColumn("", EnumColumnTypes.INT), "createColumn should throw IllegalArgumentException if column name is empty");
                assertThrows(IllegalArgumentException.class, () -> createColumn("column_name", null), "createColumn should throw IllegalArgumentException if column type is null");

                assertThrows(IllegalArgumentException.class, () -> createTextColumn(null, 255), "createTextColumn should throw IllegalArgumentException if column name is null");
                assertThrows(IllegalArgumentException.class, () -> createTextColumn("", 255), "createTextColumn should throw IllegalArgumentException if column name is empty");
                assertThrows(IllegalArgumentException.class, () -> createTextColumn("column_name", 0), "createTextColumn should throw IllegalArgumentException if size is less than or equal to 0");
                assertThrows(IllegalArgumentException.class, () -> createTextColumn("column_name", -1), "createTextColumn should throw IllegalArgumentException if size is less than or equal to 0");
            }
        };
    }
}
