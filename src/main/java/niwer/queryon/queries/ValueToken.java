package niwer.queryon.queries;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a token that can be used as a value in an insertion or update query.
 * It can be a simple parameter, a raw SQL expression, or a more complex expression with parameters.
 */
public record ValueToken(String sqlFragment, List<Object> params) {
    public static ValueToken parameter(Object value) {
        return new ValueToken("?", List.of(value));
    }

    public static ValueToken raw(String expression) {
        return new ValueToken(expression, List.of());
    }

    public static ValueToken expression(String expression, Object... expressionParams) {
        if (expressionParams == null || expressionParams.length == 0) return raw(expression);
        return new ValueToken(expression, Arrays.asList(expressionParams));
    }
}
