package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.QueryonEngineTest;
import niwer.queryon.TestUserTable;
import niwer.queryon.queries.Expression;

class ColumnTest {

    @Test void testColumnCreation(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.INT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(0, Expression.of("test_column").isLessThan(100));
        });
    }

    @Test void testColumnCreationNoDB() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Column(null, "test_column", EnumColumnTypes.INT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(0, Expression.of("test_column").isLessThan(100));
        });
    }

    @Test void testColumnCreationWithDefaultValue(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.REAL, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(25.0, Expression.of("test_column").isLessThan(100.0));
        });
    }

    @Test void testVarCharDefaultValue(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.VARCHAR, 255, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent", Expression.of("test_column").isNotEqualTo("TestContent"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.VARCHAR, 5, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent", Expression.of("test_column").isNotEqualTo("TestContent"));
        });
    }

    @Test void testTextColumnCreation(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.TEXT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("TestContent", Expression.of("test_column").isNotEqualTo("TestContent"));
        });
    }

    @Test void testColumnCreationBoolean(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.BOOLEAN, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(true, Expression.of("test_column").isEqualTo(true));
        });
    }

    @Test void testColumnCreationDate(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("CURRENT_TIMESTAMP", Expression.of("test_column").isEqualTo("CURRENT_TIMESTAMP"));
        });

        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("CURRENT_TIMESTAMP", Expression.of("test_column").isEqualTo("CURRENT_TIMESTAMP"));
        });

        {
            assertDoesNotThrow(() -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue("0000-00-00", Expression.of("test_column").isEqualTo("0000-00-00"));
            });
            
            assertDoesNotThrow(() -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue("0000-00-00 00:00:00", Expression.of("test_column").isEqualTo("0000-00-00 00:00:00"));
            });
        }
        
        {
            assertDoesNotThrow(() -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue(new java.util.Date(), Expression.of("test_column").isEqualTo("CURRENT_TIMESTAMP"));
            });

            assertDoesNotThrow(() -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue(new java.util.Date(), Expression.of("test_column").isEqualTo("CURRENT_TIMESTAMP"));
            });
        }

        {
            assertThrows(IllegalArgumentException.class, () -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue(1225, Expression.of("test_column").isLessThan(100));
            });
    
            assertThrows(IllegalArgumentException.class, () -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue(1225, Expression.of("test_column").isLessThan(100));
            });
    
            assertThrows(IllegalArgumentException.class, () -> {
                new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.DATE_TIME, 0, null)
                    .autoIncrement()
                    .notNull()
                    .unique()
                    .defaultValue("0000-00-00", Expression.of("test_column").isEqualTo("0000-00-00"));
            });
        }
    }

    @Test void testColumnCreationEnum(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.ENUM, 0, QueryonEngineTest.TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(QueryonEngineTest.TestEnum.VALUE1); // No expression supported for ENUM default values, as they must be constant
        });
    }

    @Test void testColumnCreationDefaultWithExpression(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.INT, 0, null)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(null, Expression.of("test_column").isGreaterThan(0));
        });
    }

    @Test void testColumnEnumWithInvalidDefault(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.ENUM, 0, QueryonEngineTest.TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue("INVALID_VALUE", Expression.of("test_column").isEqualTo("INVALID_VALUE"));
        });
    }

    @Test void testColumnCreationInvalidDefaultWithExpression(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.ENUM, 0, QueryonEngineTest.TestEnum.class)
                .autoIncrement()
                .notNull()
                .unique()
                .defaultValue(null, Expression.of("test_column").isGreaterThan(0));
        });
    }

    @Test void testForeignKeyDefinition(@TempDir File tempDir) {
        assertDoesNotThrow(() -> {
            new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "user_id", EnumColumnTypes.INT, 0, null)
                .foreignKey(TestUserTable.class, "id", EnumForeginKeyAction.SET_NULL)
                .foreignKey(TestUserTable.class, null, EnumForeginKeyAction.SET_NULL)
            ;
        });
    }

    @Test void testInvalidColumnCreation(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () -> new Column(null, "test_column", EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), null, EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "", EnumColumnTypes.INT, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.VARCHAR, -1, null));
        assertThrows(IllegalArgumentException.class, () -> new Column(QueryonEngineTest.setupUsersAndFoodDB(tempDir), "test_column", EnumColumnTypes.ENUM, 0, null));
    }
}
