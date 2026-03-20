package niwer.queryon;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import niwer.lumen.LumenEngine;
import niwer.lumen.container.Container;

public class QueryonEngine {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

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

    /**
     * Formats an array of objects as a comma-separated string.
     * 
     * @param escapeString Whether to escape string values with single quotes
     * @param values The objects to format
     * @return The formatted string
     */
    public static String formatValues(boolean escapeString, Object... values) {
        final String[] VALUES = Arrays.stream(values).map(t -> {
            if (t instanceof String) return escapeString ? "'" + t + "'" : (String) t;
            else if (t instanceof Date) return "'" + dateTimeToSQL((Date) t) + "'";
            else return String.valueOf(t);
        }).toArray(String[]::new);
        return formatValues(VALUES);
    }

    /**
     * Formats an array of strings as a comma-separated string.
     * 
     * @param values The strings to format
     * @return The formatted string
     */
    public static String formatValues(String... values) {
        return String.join(", ", values);
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
}
