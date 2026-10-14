package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.QueryonEngineTest.TestEnum;
import niwer.queryon.QueryonException;
import niwer.queryon.SQLSerializable;
import niwer.queryon.TestEmptyTable;
import niwer.queryon.TestFoodTable;
import niwer.queryon.TestTable;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.QueryManager;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IForeignKey;

class TableTest {

    @Test void testCreateTable(@TempDir File tempDir) {
        assertDoesNotThrow(() -> new Table(QueryonEngineTest.setupUsersDB(tempDir)) {
            @Override public String name() { return "test_table"; }
        });
    }

    @Test void testCreateEmptyTable(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new TestEmptyTable(DB);
        DB.registerTable(TestEmptyTable.class);
        assertThrows(IllegalStateException.class, () -> TABLE.execute());
    }

    @Test void testCreateTableWithColumnsAndConstraints(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table TABLE = new Table(DB) {
            @Override public String name() { return "test_table"; }
        };
        assertDoesNotThrow(() -> TABLE
            .addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT))
            .addCheckConstraint(Expressions.isEqualTo("int_column", 25))
            .execute()
        );
    }

    @Test void testAutomaticSchemaSyncMigratesChangedSchema(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "schema_sync_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT)).execute();
        QueryManager.query(DB, "INSERT INTO schema_sync_table (int_column) VALUES (25);");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "schema_sync_table"; }
        };
        UPDATED_TABLE.addColumn(Table.createColumn(DB, "int_column", EnumColumnTypes.INT))
            .addCheckConstraint(Expressions.isEqualTo("int_column", 25))
            .execute();

        final String SCHEMA = QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='schema_sync_table';");
        assertTrue(SCHEMA.contains("CHECK"), "Schema synchronization should apply changed constraints");
        assertEquals(1, QueryManager.queryInt(DB, "SELECT COUNT(*) FROM schema_sync_table;"),
            "Schema migration should preserve existing rows");
    }

    @Test void testAutomaticSchemaSyncMigratesConstraintsAndAddsColumns(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "schema_sync_combined_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "id", EnumColumnTypes.INT)).execute();

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "schema_sync_combined_table"; }
        };
        UPDATED_TABLE.addColumns(
            Table.createColumn(DB, "id", EnumColumnTypes.INT),
            Table.createColumn(DB, "status", EnumColumnTypes.TEXT)
        ).addCheckConstraint(Expressions.in("status", TestEnum.class)).execute();

        assertEquals(1, QueryManager.queryInt(DB,
            "SELECT COUNT(*) FROM pragma_table_info('schema_sync_combined_table') WHERE name='status';"));
        assertTrue(QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='schema_sync_combined_table';")
            .contains("CHECK"));

        assertDoesNotThrow(UPDATED_TABLE::execute,
            "A table matching the model should not be migrated again");
    }

    @Test void testAutomaticSchemaSyncIgnoresChangingDateTimeDefaults(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "datetime_default_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "created_at", EnumColumnTypes.DATE_TIME)
            .defaultValue(new Date(0))).execute();
        final String INITIAL_SCHEMA = QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='datetime_default_table';");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "datetime_default_table"; }
        };
        UPDATED_TABLE.addColumn(Table.createColumn(DB, "created_at", EnumColumnTypes.DATE_TIME)
            .defaultValue(new Date())).execute();

        assertEquals(INITIAL_SCHEMA, QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='datetime_default_table';"),
            "Changing a generated datetime default should not rebuild the table");
    }

    @Test void testAutomaticSchemaSyncIgnoresChangingTextDateTimeDefaults(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "text_datetime_default_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "created_at", EnumColumnTypes.TEXT)
            .defaultValue("2026-10-15 00:42:03.686")).execute();
        final String INITIAL_SCHEMA = QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='text_datetime_default_table';");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "text_datetime_default_table"; }
        };
        UPDATED_TABLE.addColumn(Table.createColumn(DB, "created_at", EnumColumnTypes.TEXT)
            .defaultValue("2026-10-15 00:46:43.390")).execute();

        assertEquals(INITIAL_SCHEMA, QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='text_datetime_default_table';"),
            "Changing a generated text datetime default should not rebuild the table");
    }

    @Test void testAutomaticSchemaSyncIgnoresChangingGeneratedStringDefaults(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "generated_string_default_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "code", EnumColumnTypes.TEXT)
            .defaultValue("36qm4pue")).execute();
        final String INITIAL_SCHEMA = QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='generated_string_default_table';");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "generated_string_default_table"; }
        };
        UPDATED_TABLE.addColumn(Table.createColumn(DB, "code", EnumColumnTypes.TEXT)
            .defaultValue("360npid6")).execute();

        assertEquals(INITIAL_SCHEMA, QueryManager.queryString(DB,
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='generated_string_default_table';"),
            "Changing a generated string default should not rebuild the table");
    }

    @Test void testAutomaticSchemaSyncAddsMissingColumn(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "missing_column_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "id", EnumColumnTypes.INT)).execute();
        QueryManager.query(DB, "INSERT INTO missing_column_table (id) VALUES (7);");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "missing_column_table"; }
        };
        UPDATED_TABLE.addColumns(
            Table.createColumn(DB, "id", EnumColumnTypes.INT),
            Table.createColumn(DB, "name", 32)
        ).execute();

        assertEquals(1, QueryManager.queryInt(DB,
            "SELECT COUNT(*) FROM pragma_table_info('missing_column_table') WHERE name='name';"));
        assertEquals(1, QueryManager.queryInt(DB, "SELECT COUNT(*) FROM missing_column_table;"),
            "Adding a column should preserve existing rows");
    }

    @Test void testAutomaticSchemaSyncRejectsIncompatibleData(@TempDir File tempDir) {
        final DataBase DB = QueryonEngineTest.setupUsersDB(tempDir);
        final Table INITIAL_TABLE = new Table(DB) {
            @Override public String name() { return "incompatible_data_table"; }
        };
        INITIAL_TABLE.addColumn(Table.createColumn(DB, "status", EnumColumnTypes.TEXT)).execute();
        QueryManager.query(DB, "INSERT INTO incompatible_data_table (status) VALUES ('REMOVED');");

        final Table UPDATED_TABLE = new Table(DB) {
            @Override public String name() { return "incompatible_data_table"; }
        };
        UPDATED_TABLE.addColumn(Table.createColumn(DB, "status", EnumColumnTypes.TEXT))
            .addCheckConstraint(Expressions.in("status", TestEnum.class));

        final QueryonException EXCEPTION = assertThrows(QueryonException.class, UPDATED_TABLE::execute);
        assertTrue(EXCEPTION.getMessage().contains("existing data is incompatible"),
            "Migration failures should explain that existing data must be updated");
        assertEquals(1, QueryManager.queryInt(DB,
            "SELECT COUNT(*) FROM incompatible_data_table WHERE status='REMOVED';"),
            "A failed migration should leave the original table and data intact");
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
        final Table TABLE = new TestTable(DB);
        DB.registerTable(TestTable.class);
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
        
        assertDoesNotThrow(() -> {
            TABLE.addColumnsFromClass(TestNonAnnotatedClass.class).execute();
        }, "Should not throw an exception when creating a column from a class without ColumnField annotations");
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
    
    @SuppressWarnings("unused")
    private static class TestNonAnnotatedClass extends SQLSerializable<TestNonAnnotatedClass> {
        private int id;
        private String name;
        private String uuid;
        private String foodId;
        private TestEnum testEnum;
        private Date testDate;
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
