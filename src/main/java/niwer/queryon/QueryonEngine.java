package niwer.queryon;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import niwer.lumen.LumenEngine;
import niwer.lumen.container.Container;

public class QueryonEngine {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final DateTimeFormatter SQL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Container LOGGER = LumenEngine.registerContainer("QUERYON");

    /**
     * Marker type for explicitly passing raw SQL fragments.
     */
    public static final class RawExpression {
        private final String sql;

        private RawExpression(String sql) {
            this.sql = sql;
        }

        @Override
        public String toString() {
            return sql;
        }
    }

    /**
     * Normalizes a SQL string by replacing multiple whitespace characters with a single space, trimming leading and trailing whitespace, and converting to lowercase.
     * This is useful for comparing SQL strings in a case-insensitive manner and ignoring formatting differences
     * 
     * @param sql The SQL string to normalize
     * @return The normalized SQL string
     */
    public static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    /**
     * Convert a Date object to String in yyyy-MM-dd format.
     * e.g : 2026-01-25
     * 
     * @param date The date object
     * @return The formatted date string
     */
    public static String dateToSQL(Date date) {
        final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
        FORMATTER.setTimeZone(UTC);
        return FORMATTER.format(date);
    }

    /**
     * Convert a LocalDate object to String in yyyy-MM-dd format.
     * 
     * @param date The LocalDate object
     * @return The formatted date string
     */
    public static String dateToSQL(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Convert a LocalDateTime object to String in yyyy-MM-dd HH:mm:ss format.
     * 
     * @param date The LocalDateTime object
     * @return The formatted date string
     */
    public static String dateTimeToSQL(LocalDateTime date) {
        return date.format(SQL_DATE_TIME_FORMATTER);
    }

    /**
     * @deprecated Use {@link #dateTimeToSQL(LocalDateTime)} instead.
     */
    @Deprecated
    public static String dateToSql(LocalDateTime date) {
        return dateTimeToSQL(date);
    }

    /**
     * Convert a Date object to String in yyyy-MM-dd HH:mm:ss format.
     * e.g : 2026-01-25 01:25:30
     * 
     * @param date The date object
     * @return The formatted date string
     */
    public static String dateTimeToSQL(Date date) {
        final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FORMATTER.setTimeZone(UTC);
        return FORMATTER.format(date);
    }

    private static String formatDate(Date date) {
        return date instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate().toString() : QueryonEngine.dateTimeToSQL(date);
    }

    private static String formatDateTimeValue(Object value) {
        return value instanceof LocalDateTime dateTime ? dateTime.format(SQL_DATE_TIME_FORMATTER) : value.toString();
    }

    /**
     * Prepares a parameter for a PreparedStatement by applying specific formatting (for Dates, Enums, etc.) before sending it to the JDBC driver.
     * 
     * @param value The value to prepare for the PreparedStatement
     * @return The prepared value, formatted as needed for the PreparedStatement
     */
    public static Object prepareParam(Object value) {
        if (value == null) return null;
        if (value instanceof Date) return formatDate((Date) value);
        if (value instanceof LocalDate || value instanceof LocalDateTime) return formatDateTimeValue(value);
        if (value instanceof Enum<?>) return value.toString();
        return value; // Strings and numbers are returned as-is
    }

    public static String toSqlLiteral(Object value) {
        Object prepared = prepareParam(value);
        if (prepared == null) return "NULL";
        if (prepared instanceof Number || prepared instanceof Boolean) return prepared.toString();
        return "'" + prepared.toString().replace("'", "''") + "'";
    }

    /**
     * Wraps a raw SQL fragment so it is inserted as-is instead of quoted as a string literal.
     *
     * @param sql The raw SQL fragment
     * @return A marker object recognized as an expression by Queryon
     */
    public static RawExpression raw(String sql) {
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("Raw SQL expression cannot be null or empty");
        return new RawExpression(sql);
    }

    /**
     * Checks if a value is an explicitly marked SQL expression.
     *
     * @param value The value to check
     * @return True if the value is a raw SQL expression marker, false otherwise
     */
    public static boolean isExpression(Object value) {
        return value instanceof RawExpression;
    }

    /**
     * Escapes double quotes in a string by replacing them with two double quotes.
     * SQL uses two double quotes to represent all columns and tables names.
     * While '' represents a literal single quote within a string.
     * 
     * @param str The string to escape
     * @return The escaped string
     */
    public static String escapeString(String str) {
        return "\"" + str + "\"";
    }

    /**
     * Wraps a primitive type in its corresponding wrapper class.
     * 
     * @param type The primitive type to wrap
     * @return The corresponding wrapper class, or the original type if it's not primitive
     */
    public static Class<?> wrap(Class<?> type) {
        if(type == null) throw new IllegalArgumentException("Type cannot be null");
        
        /* Wrap primitive types in their corresponding wrapper classes */
        if(type.isPrimitive()) {
            return switch (type.getName()) {
                case "int" -> Integer.class;
                case "long" -> Long.class;
                case "boolean" -> Boolean.class;
                case "double" -> Double.class;
                case "float" -> Float.class;
                case "byte" -> Byte.class;
                case "short" -> Short.class;
                case "char" -> Character.class;
                case "void" -> Void.class;
                default -> type; // Should never happen
            };
        }

        return type;
    }
}
