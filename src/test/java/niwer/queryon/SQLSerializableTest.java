package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import niwer.queryon.QueryonEngineTest.TestEnum;
import niwer.queryon.tables.api.IColumnField;

class SQLSerializableTest {

    @SuppressWarnings("unused")
    private static class Example {
        public TestEnum enumField = TestEnum.VALUE1;
        public String stringField = "test";
        public int intField = 42;
        public boolean booleanField = true;
        public long longField = 123456789L;
        public double doubleField = 3.14;
        public float floatField = 2.71f;
        public Date dateField = new GregorianCalendar(2000, 1, 1).getTime();

        public static String staticField = "static"; // Should be ignored
        private String privateField = "private"; // Should be ignored
    }
    @Test void testDefaultDataFromField() {
        assertEquals("test", defaultValue("stringField"));
        assertEquals(42, defaultValue("intField"));
        assertEquals("static", defaultValue("staticField"));
        assertThrows(IllegalArgumentException.class, () -> SQLSerializable.defaultDataFromField(null));
    }

    private static Object defaultValue(String name) {
        try {
            final Field FIELD = Example.class.getDeclaredField(name);
            return SQLSerializable.defaultDataFromField(FIELD);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void testObjectifyLocalDateAndLocalDateTime() throws Exception {
        final LocalDate EXPECTED_DATE = LocalDate.of(2024, 1, 1);
        final LocalDateTime EXPECTED_DATE_TIME = LocalDateTime.of(2024, 1, 1, 12, 30, 45);
        final ResultSet RESULT_SET = (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[] { ResultSet.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getDate" -> java.sql.Date.valueOf(EXPECTED_DATE);
                case "getTimestamp" -> Timestamp.valueOf(EXPECTED_DATE_TIME);
                default -> null;
            });

        final LocalDateRow ROW = new LocalDateRow();
        ROW.objectify(RESULT_SET);

        assertEquals(EXPECTED_DATE, ROW.date);
        assertEquals(EXPECTED_DATE_TIME, ROW.dateTime);
    }

    private static class LocalDateRow extends SQLSerializable<LocalDateRow> {
        @IColumnField
        private LocalDate date;

        @IColumnField
        private LocalDateTime dateTime;
    }

    @Test void testObjectifySupportedFieldTypesAndMetadata() throws Exception {
        final LocalDate DATE = LocalDate.of(2024, 1, 1);
        final LocalDateTime DATE_TIME = LocalDateTime.of(2024, 1, 1, 12, 30, 45);
        final ResultSet RESULT_SET = (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[] { ResultSet.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getInt" -> 42;
                case "getString" -> "VALUE1";
                case "getBoolean" -> true;
                case "getLong" -> 123L;
                case "getDouble" -> 3.5;
                case "getFloat" -> 2.5f;
                case "getDate" -> java.sql.Date.valueOf(DATE);
                case "getTimestamp" -> Timestamp.valueOf(DATE_TIME);
                default -> null;
            });

        final SupportedTypesRow ROW = new SupportedTypesRow();
        ROW.objectify(RESULT_SET);

        assertEquals(42, ROW.integer);
        assertEquals("VALUE1", ROW.enumValue.name());
        assertTrue(ROW.booleanValue);
        assertEquals(123L, ROW.longValue);
        assertEquals(3.5, ROW.doubleValue);
        assertEquals(2.5f, ROW.floatValue);
        assertEquals(java.sql.Date.valueOf(DATE), ROW.date);
        assertEquals(DATE, ROW.localDate);
        assertEquals(DATE_TIME, ROW.localDateTime);
        assertArrayEquals(new String[] {"renamed", "enumValue", "booleanValue", "longValue", "doubleValue", "floatValue", "date", "localDate", "localDateTime"},
            ROW.columnNames());
        assertEquals(9, ROW.valuesFromObject().length);
    }

    private static class SupportedTypesRow extends SQLSerializable<SupportedTypesRow> {
        @IColumnField(name = "renamed")
        private int integer;
        @IColumnField private TestEnum enumValue;
        @IColumnField private boolean booleanValue;
        @IColumnField private long longValue;
        @IColumnField private double doubleValue;
        @IColumnField private float floatValue;
        @IColumnField private Date date;
        @IColumnField private LocalDate localDate;
        @IColumnField private LocalDateTime localDateTime;
    }

    @Test void testEnumFromString() {
        // Test with a valid enum constant
        assertEquals(QueryonEngineTest.TestEnum.VALUE1, enumFromString(TestEnum.class, "VALUE1"));
        assertEquals(QueryonEngineTest.TestEnum.VALUE2, enumFromString(TestEnum.class, "VALUE2"));
        assertEquals(QueryonEngineTest.TestEnum.VALUE3, enumFromString(TestEnum.class, "VALUE3"));

        // Test with an invalid enum constant
        assertThrows(IllegalArgumentException.class, () -> enumFromString(TestEnum.class, "INVALID_VALUE"));

        // Test with a non-enum class
        assertThrows(IllegalArgumentException.class, () -> enumFromString(String.class, "VALUE1"));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E enumFromString(Class<?> enumClass, String value) {
        try {
            Method method = SQLSerializable.class.getDeclaredMethod("enumFromString", Class.class, String.class);
            method.setAccessible(true);
            return (E) method.invoke(null, enumClass, value);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
