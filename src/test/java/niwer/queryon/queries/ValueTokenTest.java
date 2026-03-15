package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import niwer.queryon.queries.ValueToken;

class ValueTokenTest {

    @Test void testParameter() {
        final ValueToken TOKEN = ValueToken.parameter(42);
        assertEquals("?", TOKEN.sqlFragment());
        assertEquals(List.of(42), TOKEN.params());
    }

    @Test void testRaw() {
        final ValueToken TOKEN = ValueToken.raw("CURRENT_TIMESTAMP");
        assertEquals("CURRENT_TIMESTAMP", TOKEN.sqlFragment());
        assertEquals(List.of(), TOKEN.params());
    }

    @Test void testExpression() {
        final ValueToken TOKEN = ValueToken.expression("age > ?", 18);
        assertEquals("age > ?", TOKEN.sqlFragment());
        assertEquals(List.of(18), TOKEN.params());
    }
}
