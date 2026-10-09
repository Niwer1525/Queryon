package niwer.queryon.queries;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class providing factory methods for building {@link Expression} instances.
 * Provides a declarative API for building complex expressions using logical operators (AND, OR)
 * and comparison operators (=, <>, >, <, >=, <=)[cite: 1].
 * 
 * For example you can use this class to build an expression like:
 * "(age > 30 AND name = 'Alice') OR (age <= 30 AND name = 'Bob')"[cite: 1]
 */
public final class Expressions {

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

        String combined = Arrays.stream(expressions)
            .map(Expression::toString)
            .collect(Collectors.joining(" " + operator + " "));

        return new Expression("(" + combined + ")");
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

    public static Expression isGreaterThan(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " > " + value);
    }

    public static Expression isGreaterThanOrEqualTo(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " >= " + value);
    }

    public static Expression isLessThan(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " < " + value);
    }

    public static Expression isLessThanOrEqualTo(String column, Number value) {
        validateColumn(column);
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        return new Expression(column + " <= " + value);
    }

    public static Expression isEqualTo(String column, Object value) {
        validateColumn(column);
        if (value == null) {
            return new Expression(column + " IS NULL");
        } else if (value instanceof String) {
            return new Expression(column + " = '" + escape(value.toString()) + "'");
        } else {
            return new Expression(column + " = " + value);
        }
    }

    public static Expression isNotEqualTo(String column, Object value) {
        validateColumn(column);
        if (value == null) {
            return new Expression(column + " IS NOT NULL");
        } else if (value instanceof String) {
            return new Expression(column + " <> '" + escape(value.toString()) + "'");
        } else {
            return new Expression(column + " <> " + value);
        }
    }

    public static Expression in(String column, Object... values) {
        validateColumn(column);
        if (values == null) throw new IllegalArgumentException("Values array cannot be null");
        if (values.length == 0) throw new IllegalArgumentException("Values array cannot be empty");

        final StringBuilder QUERY = new StringBuilder(column + " IN (");
        for (int i = 0; i < values.length; i++) {
            final Object VALUE = values[i];
            if (VALUE instanceof String) QUERY.append("'").append(escape(VALUE.toString())).append("'");
            else if (VALUE == null) QUERY.append("NULL");
            else QUERY.append(VALUE);
            if (i < values.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        return new Expression(QUERY.toString());
    }

    public static Expression in(String column, Class<? extends Enum<?>> enumClass) {
        validateColumn(column);
        if (enumClass == null) throw new IllegalArgumentException("Enum class cannot be null");
        if (enumClass.getEnumConstants().length == 0) throw new IllegalArgumentException("Enum class cannot be empty");

        final StringBuilder QUERY = new StringBuilder(column + " IN (");
        final Object[] ENUM_CONSTANTS = enumClass.getEnumConstants();
        for (int i = 0; i < ENUM_CONSTANTS.length; i++) {
            final Object VALUE = ENUM_CONSTANTS[i];
            QUERY.append("'").append(VALUE).append("'");
            if (i < ENUM_CONSTANTS.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        return new Expression(QUERY.toString());
    }

    /**
     * Adds a LIKE condition to the expression with the specified pattern[cite: 1].
     * The pattern can include SQL wildcards such as '%' and '_'[cite: 1].
     * Pattern examples :
     * - "A%" matches any string that starts with 'A'[cite: 1]
     * - "%A" matches any string that ends with 'A'[cite: 1]
     * - "%A%" matches any string that contains 'A'[cite: 1]
     * - "A_B" matches any string that starts with 'A', followed by any single character, and ends with 'B'[cite: 1]
     * 
     * @param column The column that the expression is based on[cite: 1]. Cannot be null or empty.
     * @param pattern The pattern to match, which can include SQL wildcards[cite: 1]. Cannot be null.
     * @return The expression defined as "COLUMN LIKE 'pattern'". If the pattern is null, an IllegalArgumentException will be thrown.
     */
    public static Expression like(String column, String pattern) {
        validateColumn(column);
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");

        return new Expression(column + " LIKE '" + escape(pattern) + "'");
    }

    /**
     * Adds a BETWEEN condition to the expression with the specified lower and upper bounds[cite: 1].
     * 
     * @param column The column that the expression is based on[cite: 1]. Cannot be null or empty.
     * @param lower The lower bound of the range[cite: 1]. Cannot be null[cite: 1].
     * @param upper The upper bound of the range[cite: 1]. Cannot be null[cite: 1].
     * @return The expression defined as "COLUMN BETWEEN lower AND upper". If either the lower or upper bound is null, an IllegalArgumentException will be thrown[cite: 1].
     */
    public static Expression between(String column, Number lower, Number upper) {
        validateColumn(column);
        if (lower == null || upper == null) throw new IllegalArgumentException("Lower and upper bounds cannot be null");

        return new Expression(column + " BETWEEN " + lower + " AND " + upper);
    }

    private static void validateColumn(String column) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty");
    }

    private static String escape(String text) {
        return text.replace("'", "''");
    }
}