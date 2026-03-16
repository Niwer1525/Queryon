package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestFoodTable;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.Expression;

class ColumnTest {

    @TempDir
    private static File tempDir;

    private static DataBase setupDataBase(String name) {
        final DataBase DB = new DataBase(new File(tempDir, name +".db")).registerTable(TestUserTable.class).registerTable(TestFoodTable.class);
        return DB;
    }

    @Test void testColumnCreation() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.INT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(0);
        });
    }

    @Test void testColumnCreationWithDefaultValue() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.REAL, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(25.0);
        });
    }

    @Test void testVarCharDefaultValue() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.VARCHAR, 255, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.VARCHAR, 5, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent");
        });
    }

    @Test void testTextColumnCreation() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.TEXT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent");
        });
    }

    @Test void testColumnCreationBoolean() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.BOOLEAN, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(true);
        });
    }

    @Test void testColumnCreationDate() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("0000-00-00");
        });

        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("0000-00-00 00:00:00");
        });

        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(new java.util.Date());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(1225);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(1225);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("0000-00-00");
        });
    }

    @Test void testColumnCreationEnum() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.ENUM, 0, TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(TestEnum.VALUE1);
        });
    }

    @Test void testColumnCreationDefaultWithExpression() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.INT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(null, Expression.of("test_column").isGreaterThan(0));
        });
    }

    @Test void testColumnEnumWithInvalidDefault() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.ENUM, 0, TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("INVALID_VALUE");
        });
    }

    @Test void testColumnCreationInvalidDefaultWithExpression() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Column(setupDataBase("test"), "test_column", EnumColumnTypes.ENUM, 0, TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(null, Expression.of("test_column").isGreaterThan(0));
        });
    }

    @Test void testForeignKeyDefinition() {
        assertDoesNotThrow(() -> {
            new Column(setupDataBase("test"), "user_id", EnumColumnTypes.INT, 0, null)
                .foreignKey(TestUserTable.class, "id", EnumForeginKeyAction.SET_NULL);
        });
    }

    @Test void testInvalidColumnCreation() {
        assertThrows(IllegalArgumentException.class, () -> new Column(null, "test_column", EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(setupDataBase("test"), null, EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(setupDataBase("test"), "", EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(setupDataBase("test"), "test_column", null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(setupDataBase("test"), "test_column", EnumColumnTypes.VARCHAR, -1, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(setupDataBase("test"), "test_column", EnumColumnTypes.ENUM, 0, null));
    }

    private enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }
}
