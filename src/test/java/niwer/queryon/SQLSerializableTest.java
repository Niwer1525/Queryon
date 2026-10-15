package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import niwer.queryon.QueryonEngineTest.TestEnum;
import niwer.queryon.tables.api.IColumnField;

class SQLSerializableTest {

    @SuppressWarnings("unused")
    private class Example {
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
        // Example example = new Example();
        // for(Field field : Example.class.getDeclaredFields()) {
        //     field.setAccessible(true);
        //     Object value = null;
        //     try {
        //         value = field.get(example);
        //     } catch (IllegalAccessException e) {
        //         throw new RuntimeException(e);
        //     }
        //     assertEquals(value, SQLSerializable.defaultDataFromField(field), "defaultDataFromField should return the correct default value for field: " + field.getName());
        // }

        // assertThrows(IllegalArgumentException.class, () -> SQLSerializable.defaultDataFromField(null));
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
