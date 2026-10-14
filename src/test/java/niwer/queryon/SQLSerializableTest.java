package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import niwer.queryon.QueryonEngineTest.TestEnum;

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
