package niwer.queryon.queries;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class represents a SQL expression that can be used in WHERE clauses, JOIN conditions, and other parts of SQL queries.
 * It provides a fluent API for building complex expressions using logical operators (AND, OR) and comparison operators (=, <>, >, <, >=, <=).
 * 
 * For example you can use this class to build an expression like "(age > 30 AND name = 'Alice') OR (age <= 30 AND name = 'Bob')" in a fluent way, and then use that expression in a query.
 */
public class Expression {
    
    private final Set<Expression> AND_EXPRESSIONS = new LinkedHashSet<>();
    private final Set<Expression> OR_EXPRESSIONS = new LinkedHashSet<>();
    private final String COLUMN; // The column that the expression is based on
    private String sql = null; // The SQL representation of the expression

    private Expression(String column) { this.COLUMN = column; }

    @Override
    public int hashCode() {
        return this.COLUMN.hashCode() + (sql != null ? sql.hashCode() : 0) + AND_EXPRESSIONS.hashCode() + OR_EXPRESSIONS.hashCode();
    }

    public static Expression of(String column) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty");
        return new Expression(column);
    }

    public Expression isNull() {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " IS NULL";
        return this;
    }

    public Expression isNotNull() {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " IS NOT NULL";
        return this;
    }

    public Expression isGreaterThan(Number value) {
        if(sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " > " + value;
        return this;
    }

    public Expression isGreaterThanOrEqualTo(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " >= " + value;
        return this;
    }

    public Expression isLessThan(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " < " + value;
        return this;
    }

    public Expression isLessThanOrEqualTo(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " <= " + value;
        return this;
    }

    public Expression isEqualTo(Object value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value instanceof String) this.sql = this.COLUMN + " = '" + value + "'";
        else if (value == null) this.sql = this.COLUMN + " = 'NULL'";
        else this.sql = this.COLUMN + " = " + value;
        return this;
    }

    public Expression isNotEqualTo(Object value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value instanceof String) this.sql = this.COLUMN + " <> '" + value + "'";
        else if (value == null) this.sql = this.COLUMN + " <> 'NULL'";
        else this.sql = this.COLUMN + " <> " + value;
        return this;
    }

    public Expression in(Object... values) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (values == null) throw new IllegalArgumentException("Values array cannot be null");
        if (values.length == 0) throw new IllegalArgumentException("Values array cannot be empty");

        final StringBuilder QUERY = new StringBuilder(this.COLUMN + " IN (");
        for (int i = 0; i < values.length; i++) {
            final Object VALUE = values[i];
            if (VALUE instanceof String) QUERY.append("'").append(VALUE).append("'");
            else if (VALUE == null) QUERY.append("'NULL'");
            else QUERY.append(VALUE);
            if (i < values.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        this.sql = QUERY.toString();
        return this;
    }

    public Expression in(Class<? extends Enum<?>> enumClass) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (enumClass == null) throw new IllegalArgumentException("Enum class cannot be null");
        if (enumClass.getEnumConstants().length == 0) throw new IllegalArgumentException("Enum class cannot be empty");

        final StringBuilder QUERY = new StringBuilder(this.COLUMN + " IN (");
        final Object[] ENUM_CONSTANTS = enumClass.getEnumConstants();
        for (int i = 0; i < ENUM_CONSTANTS.length; i++) {
            final Object VALUE = ENUM_CONSTANTS[i];
            QUERY.append("'").append(VALUE).append("'");
            if (i < ENUM_CONSTANTS.length - 1) QUERY.append(", ");
        }
        QUERY.append(")");
        this.sql = QUERY.toString();
        return this;
    }

    /**
     * Adds a LIKE condition to the expression with the specified pattern.
     * The pattern can include SQL wildcards such as '%' and '_'.
     * Pattern examples :
     * - "A%" matches any string that starts with 'A'
     * - "%A" matches any string that ends with 'A'
     * - "%A%" matches any string that contains 'A'
     * - "A_B" matches any string that starts with 'A', followed by any single character, and ends with 'B'
     * 
     * @param pattern The pattern to match, which can include SQL wildcards. Cannot be null.
     * @return The current expression instance for chaining. The expression will be defined as "COLUMN LIKE 'pattern'". If the pattern is null, an IllegalArgumentException will be thrown. If the expression is already defined, an IllegalStateException will be thrown.
     */
    public Expression like(String pattern) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");

        this.sql = this.COLUMN + " LIKE '" + pattern + "'";
        return this;
    }

    @Override
    public String toString() {
        if (sql == null) throw new IllegalStateException("Expression is not defined");

        /* Add the "AND" conditions */
        if (!AND_EXPRESSIONS.isEmpty()) {
            final String AND_SQL = AND_EXPRESSIONS.stream()
                .map(Expression::toString)
                .reduce((a, b) -> a + " AND " + b)
                .orElse("");
            sql = "(" + sql + " AND " + AND_SQL + ")";
        }

        /* Add the "OR" conditions */
        if (!OR_EXPRESSIONS.isEmpty()) {
            final String OR_SQL = OR_EXPRESSIONS.stream()
                .map(Expression::toString)
                .reduce((a, b) -> a + " OR " + b)
                .orElse("");
            sql = "(" + sql + " OR " + OR_SQL + ")";
        }

        return sql;
    }

    public Expression and(Expression other) {
        if (other == null) throw new IllegalArgumentException("Other expression cannot be null");

        this.AND_EXPRESSIONS.add(other);
        return this;
    }

    public Expression or(Expression other) {
        if (other == null) throw new IllegalArgumentException("Other expression cannot be null");
        
        this.OR_EXPRESSIONS.add(other);
        return this;
    }
}