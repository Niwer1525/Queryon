package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EnumForeginKeyActionTest {

    @Test void testEnumForeginKeyActionSql() {
        assertEquals("CASCADE", EnumForeginKeyAction.CASCADE.sql());
        assertEquals("SET NULL", EnumForeginKeyAction.SET_NULL.sql());
        assertEquals("SET DEFAULT", EnumForeginKeyAction.SET_DEFAULT.sql());
        assertEquals("NO ACTION", EnumForeginKeyAction.NO_ACTION.sql());
    }
}
