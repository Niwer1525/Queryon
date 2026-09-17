package niwer.queryon.backup;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DatabaseBackupSchedulerTest {

    @Test void testInitialization() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseBackupScheduler(0, null));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseBackupScheduler(-1, null));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseBackupScheduler(1, null));
    }
}
