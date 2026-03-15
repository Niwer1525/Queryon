package niwer.queryon.interactions;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class QueryCallbackTest {

    @Test void testNullResult() {
        new QueryCallback() {
            @Override
            public Object execute(java.sql.ResultSet result) {
                assertNull(result);
                return null; // Simulate a query that returns no results
            }
        }.execute(null); // Pass null to simulate no result set
    }
}
