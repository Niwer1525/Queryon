package niwer.queryon.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import niwer.queryon.QueryonEngineTest;
import niwer.queryon.tables.api.IColumnField;

class EnumColumnTypesTest {

    @Test void testSQLType() {
        assertEquals("BOOLEAN", EnumColumnTypes.BOOLEAN.sql());
        assertEquals("INTEGER", EnumColumnTypes.INT.sql());
        assertEquals("REAL", EnumColumnTypes.REAL.sql());
        assertEquals("DATE", EnumColumnTypes.DATE.sql());
        assertEquals("DATETIME", EnumColumnTypes.DATE_TIME.sql());
    }

    @Test void testFromJavaWithSupportedTypes() {
        assertEquals(EnumColumnTypes.ENUM, EnumColumnTypes.fromJava(QueryonEngineTest.TestEnum.class));

        assertEquals(EnumColumnTypes.TEXT, EnumColumnTypes.fromJava(String.class));
        try {
            final Field FIELD = VarCharTestClass.class.getDeclaredField("name");
            assertNotNull(FIELD);
            assertTrue(FIELD.isAnnotationPresent(IColumnField.class));
            assertEquals(255, FIELD.getAnnotation(IColumnField.class).charLimit());
            assertEquals(EnumColumnTypes.VARCHAR, EnumColumnTypes.fromJava(FIELD)); // This field has a charLimit, so it should be treated as VARCHAR
            
            final Field FIELD_2 = VarCharTestClass.class.getDeclaredField("thisFieldShouldBeText");
            assertNotNull(FIELD_2);
            assertTrue(FIELD_2.isAnnotationPresent(IColumnField.class));
            assertEquals(0, FIELD_2.getAnnotation(IColumnField.class).charLimit());
            assertEquals(EnumColumnTypes.TEXT, EnumColumnTypes.fromJava(FIELD_2)); // This field has no charLimit, so it should be treated as TEXT
            
            final Field FIELD_3 = VarCharTestClass.class.getDeclaredField("noAnnotationField");
            assertNotNull(FIELD_3);
            assertFalse(FIELD_3.isAnnotationPresent(IColumnField.class));
            assertEquals(EnumColumnTypes.TEXT, EnumColumnTypes.fromJava(FIELD_3)); // This field has no annotation, so it should be treated as TEXT
        } catch (NoSuchFieldException EX) {
            throw new RuntimeException(EX);
        }

        assertEquals(EnumColumnTypes.BOOLEAN, EnumColumnTypes.fromJava(Boolean.class));
        assertEquals(EnumColumnTypes.BOOLEAN, EnumColumnTypes.fromJava(boolean.class));
        
        assertEquals(EnumColumnTypes.INT, EnumColumnTypes.fromJava(Integer.class));
        assertEquals(EnumColumnTypes.INT, EnumColumnTypes.fromJava(int.class));
        assertEquals(EnumColumnTypes.REAL, EnumColumnTypes.fromJava(Double.class));
        assertEquals(EnumColumnTypes.REAL, EnumColumnTypes.fromJava(double.class));
        assertEquals(EnumColumnTypes.REAL, EnumColumnTypes.fromJava(Float.class));
        assertEquals(EnumColumnTypes.REAL, EnumColumnTypes.fromJava(float.class));

        assertEquals(EnumColumnTypes.DATE, EnumColumnTypes.fromJava(java.sql.Date.class));
        assertEquals(EnumColumnTypes.DATE_TIME, EnumColumnTypes.fromJava(java.sql.Timestamp.class));
    }

    @Test void testFromJavaWithUnsupportedType() {
        try {
            EnumColumnTypes.fromJava(Object.class);
        } catch (IllegalArgumentException EX) {
            assertEquals("Unsupported Java type: class java.lang.Object", EX.getMessage());
        }
    }

    private static class VarCharTestClass {
        @IColumnField(charLimit = 255)
        private String name;

        @IColumnField(charLimit = 0)
        private String thisFieldShouldBeText;

        @SuppressWarnings("unused") // Testing what happens without annotation. But the field isn't used directly so we need to suppress the unused warning
        private String noAnnotationField;
    }
}
