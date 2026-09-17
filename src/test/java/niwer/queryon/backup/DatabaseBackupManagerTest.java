package niwer.queryon.backup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.TestUserTable;

public class DatabaseBackupManagerTest {

    @Test void testBackupManagerInitialization(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        assertDoesNotThrow(() -> DatabaseBackupManager.create(DB, backupDir, "backup_", 7));
        assertDoesNotThrow(() -> DatabaseBackupManager.createWithScheduler(DB, backupDir, "backup_", 7, 60));
    }

    @Test void testEmptyOrNullPrefix(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(DB, backupDir, "", 7));
        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.createWithScheduler(DB, backupDir, "", 7, 60));

        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(DB, backupDir, null, 7));
        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.createWithScheduler(DB, backupDir, null, 7, 60));
    }

    @Test void testBackupManagerInitializationNullValues(@TempDir File tempDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(null, new File(tempDir, "backups"), "backup_", 7));
        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(DB, new File(tempDir, "backups.bkp"), "backup_", 7), "The backup directory is not properly configured or accessible");
        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(DB, null, "backup_", 7));
        assertThrows(IllegalArgumentException.class, () -> DatabaseBackupManager.create(DB, new File(tempDir, "backups"), null, 7));
    }

    @Test void testGetScheduler(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        final DatabaseBackupManager managerWithoutScheduler = DatabaseBackupManager.create(DB, backupDir, "backup_", 7);
        assertDoesNotThrow(() -> managerWithoutScheduler.scheduler());
        assertNull(managerWithoutScheduler.scheduler());
        
        final DatabaseBackupManager managerWithScheduler = DatabaseBackupManager.createWithScheduler(DB, backupDir, "backup_", 7, 60);
        assertDoesNotThrow(() -> managerWithScheduler.scheduler());
        assertNotNull(managerWithScheduler.scheduler());
        assertDoesNotThrow(() -> managerWithScheduler.scheduler().stop());
        assertDoesNotThrow(() -> managerWithScheduler.scheduler().start());
    }

    @Test void testCreateBackup(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        final DatabaseBackupManager manager = DatabaseBackupManager.create(DB, backupDir, "backup_", 7);
        assertDoesNotThrow(() -> manager.createBackup());
    }

    @Test void testCreateBackupWithScheduler(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        final DatabaseBackupManager manager = DatabaseBackupManager.createWithScheduler(DB, backupDir, "backup_", 7, 1);
        assertDoesNotThrow(() -> manager.createBackup());
    }

    @Test void testDeletedOldBackups(@TempDir File tempDir, @TempDir File backupDir) {
        final DataBase DB = new DataBase(new File(tempDir, "test.db"));
        DB.registerTable(TestUserTable.class);

        final DatabaseBackupManager manager = DatabaseBackupManager.create(DB, backupDir, "backup_", 7);
        assertDoesNotThrow(() -> manager.deletedOldBackups());
    }
}
