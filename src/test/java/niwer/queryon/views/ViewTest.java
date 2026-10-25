package niwer.queryon.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngineTest;
import niwer.queryon.queries.QueryManager;

class ViewTest {

    @Test void dropsExistingViewAndReturnsSameInstance(@TempDir File tempDir) {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        final TestView view = new TestView(db);
        QueryManager.query(db, "CREATE VIEW test_view AS SELECT id FROM user_table;");

        assertSame(view, view.dropView());
        assertFalse(QueryManager.queryHasResult(db,
            "SELECT 1 FROM sqlite_master WHERE type = 'view' AND name = ?;", view.name()));
    }

    private static final class TestView extends View {
        private TestView(DataBase db) {
            super(db);
        }

        @Override public String name() {
            return "test_view";
        }
    }
}
