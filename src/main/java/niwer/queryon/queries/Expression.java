package niwer.queryon.queries;

import java.util.Objects;

/**
 * This class represents a SQL expression that can be used in WHERE clauses, JOIN conditions, and other parts of SQL queries[cite: 1].
 */
public class Expression {

    private final String sql; // The SQL representation of the expression[cite: 1]

    Expression(String sql) {
        if (sql == null || sql.trim().isEmpty()) throw new IllegalArgumentException("SQL expression cannot be null or empty");
        this.sql = sql;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.sql);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Expression that = (Expression) obj;
        return Objects.equals(this.sql, that.sql);
    }

    @Override
    public String toString() {
        return this.sql;
    }
}