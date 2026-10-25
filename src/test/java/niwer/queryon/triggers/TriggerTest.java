package niwer.queryon.triggers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.queries.QueryManager;

class TriggerTest {

    @Test void dropsExistingTriggerAndReturnsSameInstance(@TempDir File tempDir) {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        final TestTrigger trigger = new TestTrigger(db);
        QueryManager.query(db, "CREATE TRIGGER test_trigger AFTER INSERT ON user_table BEGIN SELECT 1; END;");

        assertSame(trigger, trigger.dropTrigger());
        assertTrue(!QueryManager.queryHasResult(db,
            "SELECT 1 FROM sqlite_master WHERE type = 'trigger' AND name = ?;", trigger.name()));
    }

    private static final class TestTrigger extends Trigger {
        private TestTrigger(DataBase db) {
            super(db);
        }

        @Override public String name() {
            return "test_trigger";
        }
    }
}
