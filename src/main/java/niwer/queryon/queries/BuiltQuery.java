package niwer.queryon.queries;

import java.util.Collection;

/**
 * Represents a built SQL query with its associated parameters, ready for execution.
 * This record is used to encapsulate the SQL string and its parameters after being built by the
 */
public record BuiltQuery(String sql, Collection<Object> params) {

    public Object[] asArray() {
        return params.toArray();
    }
}
