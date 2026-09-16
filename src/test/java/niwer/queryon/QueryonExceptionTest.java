package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class QueryonExceptionTest {

    @Test void testQueryonException() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Cause of the exception");

        QueryonException exceptionWithMessage = new QueryonException(message);
        assertEquals(message, exceptionWithMessage.getMessage());
        assertNull(exceptionWithMessage.getCause());

        QueryonException exceptionWithCause = new QueryonException(message, cause);
        assertEquals(message, exceptionWithCause.getMessage());
        assertEquals(cause, exceptionWithCause.getCause());
    }
}
