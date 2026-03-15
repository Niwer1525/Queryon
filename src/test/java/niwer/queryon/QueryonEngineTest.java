package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

class QueryonEngineTest {

    @Test void testDateToSQL() {
        final Date DATE = new Date(0); // January 1, 1970
        final String SQL_DATE = QueryonEngine.dateToSQL(DATE);
        assertEquals("1970-01-01", SQL_DATE);
    }

    @Test void testDateTimeToSQL() {
        final Date DATE = new Date(0); // January 1, 1970
        final String SQL_DATE_TIME = QueryonEngine.dateTimeToSQL(DATE);
        assertEquals("1970-01-01 01:00:00", SQL_DATE_TIME);
    }
}
