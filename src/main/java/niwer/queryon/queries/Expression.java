package niwer.queryon.queries;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import niwer.queryon.QueryonEngine;

/**
 * This class represents a SQL expression that can be used in WHERE clauses, JOIN conditions, and other parts of SQL queries
 */
public class Expression {

    private final String sql; // The SQL representation of the expression
    private final List<Object> parameters; // The parameters for the expression, if any

    Expression(String sql, Object... parameters) {
        this(sql, parameters != null ? Stream.of(parameters).toList() : Collections.emptyList());
    }

    Expression(String sql, List<Object> parameters) {
        if (sql == null || sql.trim().isEmpty()) throw new IllegalArgumentException("SQL expression cannot be null or empty");
        this.sql = sql;
        this.parameters = parameters != null ? parameters.stream().map(t -> QueryonEngine.prepareParam(t)).toList() : Collections.emptyList();
    }

    public List<Object> parameters() { return List.copyOf(this.parameters); }

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