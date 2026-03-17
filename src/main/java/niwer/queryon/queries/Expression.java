package niwer.queryon.queries;

import java.util.LinkedHashSet;
import java.util.Set;

import niwer.queryon.tables.api.EnumExpressionCondition;
import niwer.queryon.tables.api.IExpression;

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
    
    public final static Expression of(IExpression expression) {
        if (expression == null) throw new IllegalArgumentException("Expression cannot be null");
        if (expression.values().length > 0 && (expression.condition() == EnumExpressionCondition.IS_NULL || expression.condition() == EnumExpressionCondition.IS_NOT_NULL))
            throw new IllegalArgumentException("IS NULL and IS NOT NULL conditions cannot have values");

        final Expression EXPRESSION = new Expression(expression.column());
        if(expression.condition() == EnumExpressionCondition.IS_NULL) return EXPRESSION.isNull();
        if(expression.condition() == EnumExpressionCondition.IS_NOT_NULL) return EXPRESSION.isNotNull();

        /* Greater Than */
        if(expression.condition() == EnumExpressionCondition.GREATER_THAN)
            return EXPRESSION.isGreaterThan(parseSingleNumberValue(expression.values(), EnumExpressionCondition.GREATER_THAN)[0]);
        if(expression.condition() == EnumExpressionCondition.GREATER_OR_EQUAL)
            return EXPRESSION.isGreaterThanOrEqualTo(parseSingleNumberValue(expression.values(), EnumExpressionCondition.GREATER_OR_EQUAL)[0]);

        /* Less Than */
        if(expression.condition() == EnumExpressionCondition.LESS_THAN)
            return EXPRESSION.isLessThan(parseSingleNumberValue(expression.values(), EnumExpressionCondition.LESS_THAN)[0]);
        if(expression.condition() == EnumExpressionCondition.LESS_OR_EQUAL)
            return EXPRESSION.isLessThanOrEqualTo(parseSingleNumberValue(expression.values(), EnumExpressionCondition.LESS_OR_EQUAL)[0]);

        /* Equal / Not Equal */
        if(expression.condition() == EnumExpressionCondition.EQUAL)
            return EXPRESSION.isEqualTo(expression.values()[0]);
        if(expression.condition() == EnumExpressionCondition.NOT_EQUAL)
            return EXPRESSION.isNotEqualTo(expression.values()[0]);

        /* Other */
        if(expression.condition() == EnumExpressionCondition.IN)
            return EXPRESSION.in((Object[])expression.values());

        if(expression.condition() == EnumExpressionCondition.LIKE)
            return EXPRESSION.like(expression.values()[0]);

        if(expression.condition() == EnumExpressionCondition.BETWEEN) {
            final Number[] VALUES = parseSingleNumberValue(expression.values(), EnumExpressionCondition.BETWEEN);
            return EXPRESSION.between(VALUES[0], VALUES[1]);
        }

       throw new IllegalArgumentException("Unsupported expression condition: " + expression.condition());
    }

    private final static Number[] parseSingleNumberValue(String[] values, EnumExpressionCondition condition) {
        if (values.length != 1) throw new IllegalArgumentException(condition + " condition requires exactly 1 value");
        try {
            final Number[] PARSED_VALUES = new Number[1];
            for (int i = 0; i < values.length; i++) {
                final String VALUE = values[i];
                if (VALUE.contains(".")) PARSED_VALUES[i] = Double.parseDouble(VALUE);
                else PARSED_VALUES[i] = Long.parseLong(VALUE);
            }
            return PARSED_VALUES;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(condition + " condition value must be a valid number");
        }
    }

    public final static Expression of(String column) {
        if (column == null || column.isEmpty()) throw new IllegalArgumentException("Column name cannot be null or empty");
        return new Expression(column);
    }

    public final Expression isNull() {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " IS NULL";
        return this;
    }

    public final Expression isNotNull() {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        this.sql = this.COLUMN + " IS NOT NULL";
        return this;
    }

    public final Expression isGreaterThan(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        this.sql = this.COLUMN + " > " + value;
        return this;
    }

    public final Expression isGreaterThanOrEqualTo(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        this.sql = this.COLUMN + " >= " + value;
        return this;
    }

    public final Expression isLessThan(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        this.sql = this.COLUMN + " < " + value;
        return this;
    }

    public final Expression isLessThanOrEqualTo(Number value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value == null) throw new IllegalArgumentException("Value cannot be null");
        this.sql = this.COLUMN + " <= " + value;
        return this;
    }

    public final Expression isEqualTo(Object value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value instanceof String) this.sql = this.COLUMN + " = '" + value + "'";
        else if (value == null) this.sql = this.COLUMN + " = 'NULL'";
        else this.sql = this.COLUMN + " = " + value;
        return this;
    }

    public final Expression isNotEqualTo(Object value) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (value instanceof String) this.sql = this.COLUMN + " <> '" + value + "'";
        else if (value == null) this.sql = this.COLUMN + " <> 'NULL'";
        else this.sql = this.COLUMN + " <> " + value;
        return this;
    }

    public final Expression in(Object... values) {
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

    public final Expression in(Class<? extends Enum<?>> enumClass) {
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
    public final Expression like(String pattern) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");

        this.sql = this.COLUMN + " LIKE '" + pattern + "'";
        return this;
    }

    /**
     * Adds a BETWEEN condition to the expression with the specified lower and upper bounds.
     * 
     * @param lower The lower bound of the range. Cannot be null.
     * @param upper The upper bound of the range. Cannot be null.
     * @return The current expression instance for chaining. The expression will be defined as "COLUMN BETWEEN lower AND upper". If either the lower or upper bound is null, an IllegalArgumentException will be thrown. If the expression is already defined, an IllegalStateException will be thrown.
     */
    public final Expression between(Number lower, Number upper) {
        if (sql != null) throw new IllegalStateException("Expression is already defined");
        if (lower == null || upper == null) throw new IllegalArgumentException("Lower and upper bounds cannot be null");

        this.sql = this.COLUMN + " BETWEEN " + lower + " AND " + upper;
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

    public final Expression and(Expression other) {
        if (other == null) throw new IllegalArgumentException("Other expression cannot be null");

        this.AND_EXPRESSIONS.add(other);
        return this;
    }

    public final Expression or(Expression other) {
        if (other == null) throw new IllegalArgumentException("Other expression cannot be null");
        
        this.OR_EXPRESSIONS.add(other);
        return this;
    }
}