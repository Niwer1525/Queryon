package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.tables.Table;

class DataBaseSyncTest {

    @Test void fetchesLiveTablesAndColumns(@TempDir File tempDir) {
        final DataBase db = new DataBase(new File(tempDir, "schema.db"))
            .registerTable(TestUserTable.class);
        final Table table = db.getTable(TestUserTable.class);

        assertTrue(DataBaseSync.fetchLiveTableNames(db).contains(table.name()));
        assertEquals(table.getRegisteredColumnNames(),
            new java.util.LinkedHashSet<>(DataBaseSync.fetchLiveColumns(db, table)));
    }

    @Test void returnsEmptyColumnsForMissingTable(@TempDir File tempDir) {
        final DataBase db = new DataBase(new File(tempDir, "missing.db"));
        final Table table = new Table(db) {
            @Override public String name() {
                return "missing_table";
            }
        };

        assertEquals(List.of(), DataBaseSync.fetchLiveColumns(db, table));
    }
}
