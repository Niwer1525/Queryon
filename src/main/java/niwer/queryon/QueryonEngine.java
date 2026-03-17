package niwer.queryon;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import niwer.lumen.LumenEngine;
import niwer.lumen.container.Container;

public class QueryonEngine {

    public static final Container LOGGER = LumenEngine.registerContainer("QUERYON");

    /**
     * Convert a Date object to String in yyyy-MM-dd format.
     * e.g : 2026-01-25
     * 
     * @param date The date object
     * @return The formatted date string
     */
    public static String dateToSQL(Date date) {
        final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
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
}
