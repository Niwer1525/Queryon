package niwer.queryon.queries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import niwer.queryon.QueryonEngine;

/**
 * Utility class providing factory methods for building {@link Expression} instances.
 * Provides a declarative API for building complex expressions using logical operators (AND, OR)
 * and comparison operators (=, <>, >, <, >=, <=).
 */
public final class Expressions {

    private static final DateTimeFormatter SQL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Expressions() {}

    // --- Logical Operators ---

    public static Expression and(Expression... expressions) {
        return combine("AND", expressions);
    }

    public static Expression or(Expression... expressions) {
        return combine("OR", expressions);
    }

    private static Expression combine(String operator, Expression... expressions) {
        if (expressions == null || expressions.length == 0) throw new IllegalArgumentException("Expressions array cannot be null or empty");
        if (expressions.length == 1) return expressions[0];

        List<String> sqlParts = new ArrayList<>();
        List<Object> combinedParams = new ArrayList<>();

        for (Expression expr : expressions) {
            sqlParts.add(expr.toString());
            combinedParams.addAll(expr.parameters()); // Gather parameters from each expression
        }

        String combinedSql = "(" + String.join(" " + operator + " ", sqlParts) + ")";
        return new Expression(combinedSql, combinedParams);
    }

    // --- Comparison Operators ---

    public static Expression isNull(String column) {
        validateColumn(column);
        return new Expression(column + " IS NULL");
    }

    public static Expression isNotNull(String column) {
        validateColumn(column);
        return new Expression(column + " IS NOT NULL");
    }

    public static Expression isGreaterThan(String column, Date date) {
        validateColumn(column);
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        return new Expression(column + " > ?", date);
    }

    public static Expression isGreaterThan(String column, LocalDate date) {
        return dateComparison(column, ">", date);
    }

    public static Expression isGreaterThan(String column, LocalDateTime dateTime) {
        return dateComparison(column, ">", dateTime);
    }
    
    public static Expression isGreaterThan(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " > ?", value);
    }

    public static Expression isGreaterThanOrEqualTo(String column, Date date) {
        validateColumn(column);
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        return new Expression(column + " >= ?", date);
    }

    public static Expression isGreaterThanOrEqualTo(String column, LocalDate date) {
        return dateComparison(column, ">=", date);
    }

    public static Expression isGreaterThanOrEqualTo(String column, LocalDateTime dateTime) {
        return dateComparison(column, ">=", dateTime);
    }

    public static Expression isGreaterThanOrEqualTo(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " >= ?", value);
    }

    public static Expression isLessThan(String column, Date date) {
        validateColumn(column);
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        return new Expression(column + " < ?", date);
    }

    public static Expression isLessThan(String column, LocalDate date) {
        return dateComparison(column, "<", date);
    }

    public static Expression isLessThan(String column, LocalDateTime dateTime) {
        return dateComparison(column, "<", dateTime);
    }

    public static Expression isLessThan(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " < ?", value);
    }

    public static Expression isLessThanOrEqualTo(String column, Date date) {
        validateColumn(column);
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        return new Expression(column + " <= ?", date);
    }

    public static Expression isLessThanOrEqualTo(String column, LocalDate date) {
        return dateComparison(column, "<=", date);
    }

    public static Expression isLessThanOrEqualTo(String column, LocalDateTime dateTime) {
        return dateComparison(column, "<=", dateTime);
    }

    public static Expression isLessThanOrEqualTo(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " <= ?", value);
    }

    public static Expression isEqualTo(String column, Object value) {
        validateColumn(column);
        if (value == null) return new Expression(column + " IS NULL"); 
        return new Expression(column + " = ?", QueryonEngine.prepareParam(value));
    }

    public static Expression isNotEqualTo(String column, Object value) {
        validateColumn(column);
        if (value == null) return new Expression(column + " IS NOT NULL");
        return new Expression(column + " <> ?", QueryonEngine.prepareParam(value));
    }

    public static Expression in(String column, Object... values) {
        validateColumn(column);
        if (values == null) throw new IllegalArgumentException("Values array cannot be null");
        if (values.length == 0) throw new IllegalArgumentException("Values array cannot be empty");

        final StringBuilder QUERY = new StringBuilder(column + " IN (");
        final List<Object> PARAMS = new ArrayList<>();
        
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) QUERY.append("NULL");
            else {
                QUERY.append("?");
                PARAMS.add(QueryonEngine.prepareParam(values[i]));
            }
            if (i < values.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        return new Expression(QUERY.toString(), PARAMS);
    }

    public static Expression in(String column, Class<? extends Enum<?>> enumClass) {
        validateColumn(column);
        if (enumClass == null) throw new IllegalArgumentException("Enum class cannot be null");
        if (enumClass.getEnumConstants().length == 0) throw new IllegalArgumentException("Enum class cannot be empty");

        final StringBuilder QUERY = new StringBuilder(column + " IN (");
        final List<Object> PARAMS = new ArrayList<>();
        final Object[] ENUM_CONSTANTS = enumClass.getEnumConstants();
        
        for (int i = 0; i < ENUM_CONSTANTS.length; i++) {
            QUERY.append("?");
            PARAMS.add(QueryonEngine.prepareParam(ENUM_CONSTANTS[i]));
            if (i < ENUM_CONSTANTS.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        return new Expression(QUERY.toString(), PARAMS);
    }

    /**
     * Adds a LIKE condition to the expression with the specified pattern.
     */
    public static Expression like(String column, String pattern) {
        validateColumn(column);
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");
        return new Expression(column + " LIKE ?", pattern);
    }

    /**
     * Adds a BETWEEN condition to the expression with the specified lower and upper bounds.
     */
    public static Expression between(String column, Number lower, Number upper) {
        validateColumn(column);
        if (lower == null || upper == null) throw new IllegalArgumentException("Lower and upper bounds cannot be null");

        return new Expression(column + " BETWEEN ? AND ?", lower, upper);
    }

    // --- Helper Methods ---

    private static void validateColumn(String column) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty");
    }

    private static Expression dateComparison(String column, String operator, LocalDate date) {
        validateColumn(column);
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        return new Expression(column + " " + operator + " ?", date.toString());
    }

    private static Expression dateComparison(String column, String operator, LocalDateTime dateTime) {
        validateColumn(column);
        if (dateTime == null) throw new IllegalArgumentException("Date-time cannot be null");
        return new Expression(column + " " + operator + " ?", dateTime.format(SQL_DATE_TIME_FORMATTER));
    }
}