package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExpressionTest {

    private static void assertExpressionEquals(String expectedSql, Expression expression) {
        assertEquals(expectedSql, expression.toString());
    }

    @Test void createGreaterThanExpression() {
        final Expression EXPRESSION = Expression.of("age").isGreaterThan(30);
        assertExpressionEquals("age > 30", EXPRESSION);
    }

    @Test void createGreaterThanOrEqualToExpression() {
        final Expression EXPRESSION = Expression.of("age").isGreaterThanOrEqualTo(30);
        assertExpressionEquals("age >= 30", EXPRESSION);
    }

    @Test void createLessThanExpression() {
        final Expression EXPRESSION = Expression.of("age").isLessThan(30);
        assertExpressionEquals("age < 30", EXPRESSION);
    }

    @Test void createLessThanOrEqualToExpression() {
        final Expression EXPRESSION = Expression.of("age").isLessThanOrEqualTo(30);
        assertExpressionEquals("age <= 30", EXPRESSION);
    }

    @Test void createEqualToExpression() {
        final Expression EXPRESSION = Expression.of("name").isEqualTo("Alice");
        assertExpressionEquals("name = 'Alice'", EXPRESSION);

        final Expression EXPRESSION_NULL = Expression.of("name").isEqualTo(null);
        assertExpressionEquals("name = 'NULL'", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expression.of("name").isEqualTo(25);
        assertExpressionEquals("name = 25", EXPRESSION_OTHER);
    }

    @Test void createNotEqualToExpression() {
        final Expression EXPRESSION = Expression.of("name").isNotEqualTo("Alice");
        assertExpressionEquals("name <> 'Alice'", EXPRESSION);

        final Expression EXPRESSION_NULL = Expression.of("name").isNotEqualTo(null);
        assertExpressionEquals("name <> 'NULL'", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expression.of("name").isNotEqualTo(25);
        assertExpressionEquals("name <> 25", EXPRESSION_OTHER);
    }

    @Test void createExpressionGetNullSQL() {
        assertThrows(IllegalStateException.class, Expression.of("email")::toString);
    }

    @Test void createExpression() {
        final Expression EXPRESSION = Expression.of("age").isGreaterThan(30);
        assertExpressionEquals("age > 30", EXPRESSION);
    }

    @Test void createStringExpression() {
        final Expression EXPRESSION = Expression.of("name").isEqualTo("Alice");
        assertExpressionEquals("name = 'Alice'", EXPRESSION);
    }

    @Test void createNullExpression() {
        final Expression EXPRESSION = Expression.of("email").isNull();
        assertExpressionEquals("email IS NULL", EXPRESSION);
    }

    @Test void createNotNullExpression() {
        final Expression EXPRESSION = Expression.of("email").isNotNull();
        assertExpressionEquals("email IS NOT NULL", EXPRESSION);
    }

    @Test void createAlreadyDefinedExpresion() {
        final Expression EXPRESSION = Expression.of("age").isGreaterThan(30);
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isLessThanOrEqualTo(40));
    }

    @Test void createInExpression() {
        final Expression EXPRESSION = Expression.of("status").in(TestStatus.class);
        assertExpressionEquals("status IN ('ACTIVE', 'INACTIVE', 'PENDING')", EXPRESSION);
    }

    @Test void createInExpressionWithNullEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of("status").in((Class<? extends Enum<?>>)null));
    }

    @Test void createInExpressionWithEmptyEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of("status").in(EmptyEnum.class));
    }

    private enum EmptyEnum {}

    private enum TestStatus {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    @Test void createInExpressionWithValues() {
        final Expression EXPRESSION = Expression.of("id").in(1, 2, 3);
        assertExpressionEquals("id IN (1, 2, 3)", EXPRESSION);
    }

    @Test void createInExpressionWithStringValues() {
        final Expression EXPRESSION = Expression.of("name").in("Alice", "Bob", "Charlie");
        assertExpressionEquals("name IN ('Alice', 'Bob', 'Charlie')", EXPRESSION);
    }

    @Test void createInExpressionWithNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of("id").in((Object[])null));
    }

    @Test void createInExpressionWithEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of("id").in());
    }

    @Test void createInExpressionWithMixedValues() {
        Expression expression = Expression.of("id").in(1, "two", 3);
        assertExpressionEquals("id IN (1, 'two', 3)", expression);
    }

    @Test void createInExpressionWithMixedStringValues() {
        final Expression EXPRESSION = Expression.of("name").in("Alice", null, "Charlie");
        assertExpressionEquals("name IN ('Alice', 'NULL', 'Charlie')", EXPRESSION);
    }

    @Test void testLikeExpressionWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of("name").like(null));
    }

    @Test void testLikeExpression() {
        final Expression EXPRESSION = Expression.of("name").like("A%");
        assertExpressionEquals("name LIKE 'A%'", EXPRESSION);

        final Expression EXPRESSION2 = Expression.of("name").like("%son");
        assertExpressionEquals("name LIKE '%son'", EXPRESSION2);

        final Expression EXPRESSION3 = Expression.of("name").like("%ann%");
        assertExpressionEquals("name LIKE '%ann%'", EXPRESSION3);
    }

    @Test void testEmptyNullAnd() {
        final Expression EXPRESSION1 = Expression.of("age").isGreaterThan(30);
        assertThrows(IllegalArgumentException.class, () -> EXPRESSION1.and(null));
        assertThrows(IllegalArgumentException.class, () -> EXPRESSION1.or(null));
    }

    @Test void testAndExpression() {
        final Expression EXPRESSION1 = Expression.of("age").isGreaterThan(30);
        final Expression EXPRESSION2 = Expression.of("status").isEqualTo("ACTIVE");
        final Expression AND_EXPRESSION = EXPRESSION1.and(EXPRESSION2);
        assertExpressionEquals("(age > 30 AND status = 'ACTIVE')", AND_EXPRESSION);
    }

    @Test void testOrExpression() {
        final Expression EXPRESSION1 = Expression.of("age").isGreaterThan(30);
        final Expression EXPRESSION2 = Expression.of("status").isEqualTo("ACTIVE");
        final Expression OR_EXPRESSION = EXPRESSION1.or(EXPRESSION2);
        assertExpressionEquals("(age > 30 OR status = 'ACTIVE')", OR_EXPRESSION);
    }

    @Test void testComplexExpression() {
        final Expression EXPRESSION1 = Expression.of("age").isGreaterThan(30);
        final Expression EXPRESSION2 = Expression.of("status").isEqualTo("ACTIVE");
        final Expression EXPRESSION3 = Expression.of("name").isEqualTo("Alice");
        final Expression COMPLEX_EXPRESSION = EXPRESSION1.and(EXPRESSION2).or(EXPRESSION3);
        assertExpressionEquals("((age > 30 AND status = 'ACTIVE') OR name = 'Alice')", COMPLEX_EXPRESSION);
    }

    @Test void testExpressionAlreadyDefined() {
        final Expression EXPRESSION = Expression.of("age").isGreaterThan(30);

        assertThrows(IllegalStateException.class, () -> EXPRESSION.isNull());
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isNotNull());
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isGreaterThan(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isGreaterThanOrEqualTo(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isLessThan(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isLessThanOrEqualTo(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isEqualTo(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.isNotEqualTo(30));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.in("Alice", "Bob"));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.in(TestStatus.class));
        assertThrows(IllegalStateException.class, () -> EXPRESSION.like("A%"));
    }

    @Test void testEmptyNullColumnName() {
        assertThrows(IllegalArgumentException.class, () -> Expression.of(null));
        assertThrows(IllegalArgumentException.class, () -> Expression.of(""));
    }

    @Test void hashCodeTest() {
        final Expression EXPRESSION1 = Expression.of("age").isGreaterThan(30);
        final Expression EXPRESSION2 = Expression.of("age").isGreaterThan(30);
        assertEquals(EXPRESSION1.hashCode(), EXPRESSION2.hashCode());

        final Expression EXPRESSION3 = Expression.of("age");
        final Expression EXPRESSION4 = Expression.of("age");
        assertEquals(EXPRESSION3.hashCode(), EXPRESSION4.hashCode());
    }
}