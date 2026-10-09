package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExpressionTest {

    private static void assertExpressionEquals(String expectedSql, Expression expression) {
        assertEquals(expectedSql, expression.toString());
    }

    @Test void createGreaterThanExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThan("age", 30);
        assertExpressionEquals("age > 30", EXPRESSION);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThan("age", null));
    }

    @Test void createGreaterThanOrEqualToExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThanOrEqualTo("age", 30);
        assertExpressionEquals("age >= 30", EXPRESSION);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThanOrEqualTo("age", null));
    }

    @Test void createLessThanExpression() {
        final Expression EXPRESSION = Expressions.isLessThan("age", 30);
        assertExpressionEquals("age < 30", EXPRESSION);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThan("age", null));
    }

    @Test void createLessThanOrEqualToExpression() {
        final Expression EXPRESSION = Expressions.isLessThanOrEqualTo("age", 30);
        assertExpressionEquals("age <= 30", EXPRESSION);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThanOrEqualTo("age", null));
    }

    @Test void createEqualToExpression() {
        final Expression EXPRESSION = Expressions.isEqualTo("name", "Alice");
        assertExpressionEquals("name = 'Alice'", EXPRESSION);

        final Expression EXPRESSION_NULL = Expressions.isEqualTo("name", null);
        assertExpressionEquals("name IS NULL", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expressions.isEqualTo("name", 25);
        assertExpressionEquals("name = 25", EXPRESSION_OTHER);
    }

    @Test void createNotEqualToExpression() {
        final Expression EXPRESSION = Expressions.isNotEqualTo("name", "Alice");
        assertExpressionEquals("name <> 'Alice'", EXPRESSION);

        final Expression EXPRESSION_NULL = Expressions.isNotEqualTo("name", null);
        assertExpressionEquals("name IS NOT NULL", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expressions.isNotEqualTo("name", 25);
        assertExpressionEquals("name <> 25", EXPRESSION_OTHER);
    }

    @Test void createExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThan("age", 30);
        assertExpressionEquals("age > 30", EXPRESSION);
    }

    @Test void createStringExpression() {
        final Expression EXPRESSION = Expressions.isEqualTo("name", "Alice");
        assertExpressionEquals("name = 'Alice'", EXPRESSION);
    }

    @Test void createNullExpression() {
        final Expression EXPRESSION = Expressions.isNull("email");
        assertExpressionEquals("email IS NULL", EXPRESSION);
    }

    @Test void createNotNullExpression() {
        final Expression EXPRESSION = Expressions.isNotNull("email");
        assertExpressionEquals("email IS NOT NULL", EXPRESSION);
    }

    @Test void createInExpression() {
        final Expression EXPRESSION = Expressions.in("status", TestStatus.class);
        assertExpressionEquals("status IN ('ACTIVE', 'INACTIVE', 'PENDING')", EXPRESSION);
    }

    @Test void createInExpressionWithNullEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status", (Class<? extends Enum<?>>)null));
    }

    @Test void createInExpressionWithEmptyEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status", EmptyEnum.class));
    }

    private enum EmptyEnum {}

    private enum TestStatus {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    @Test void createInExpressionWithValues() {
        final Expression EXPRESSION = Expressions.in("id", 1, 2, 3);
        assertExpressionEquals("id IN (1, 2, 3)", EXPRESSION);
    }

    @Test void createInExpressionWithStringValues() {
        final Expression EXPRESSION = Expressions.in("name", "Alice", "Bob", "Charlie");
        assertExpressionEquals("name IN ('Alice', 'Bob', 'Charlie')", EXPRESSION);
    }

    @Test void createInExpressionWithNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("id", (Object[])null));
    }

    @Test void createInExpressionWithEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("id"));
    }

    @Test void createInExpressionWithMixedValues() {
        Expression expression = Expressions.in("id", 1, "two", 3);
        assertExpressionEquals("id IN (1, 'two', 3)", expression);
    }

    @Test void createInExpressionWithMixedStringValues() {
        final Expression EXPRESSION = Expressions.in("name", "Alice", null, "Charlie");
        assertExpressionEquals("name IN ('Alice', NULL, 'Charlie')", EXPRESSION);
    }

    @Test void testLikeExpressionWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.like("name", null));
    }

    @Test void testLikeExpression() {
        final Expression EXPRESSION = Expressions.like("name", "A%");
        assertExpressionEquals("name LIKE 'A%'", EXPRESSION);

        final Expression EXPRESSION2 = Expressions.like("name", "%son");
        assertExpressionEquals("name LIKE '%son'", EXPRESSION2);

        final Expression EXPRESSION3 = Expressions.like("name", "%ann%");
        assertExpressionEquals("name LIKE '%ann%'", EXPRESSION3);
    }

    @Test void testBetweenExpression() {
        final Expression EXPRESSION3 = Expressions.between("name", 25, 35);
        assertExpressionEquals("name BETWEEN 25 AND 35", EXPRESSION3);
    }

    @Test void testBetweenExpressionWithNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.between("name", null, 35));
        assertThrows(IllegalArgumentException.class, () -> Expressions.between("name", 25, null));
    }

    @Test void testAndExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        assertExpressionEquals("(age > 30 AND status = 'ACTIVE')", Expressions.and(EXPRESSION1, EXPRESSION2));
    }

    @Test void testOrExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        assertExpressionEquals("(age > 30 OR status = 'ACTIVE')", Expressions.or(EXPRESSION1, EXPRESSION2));
    }

    @Test void testComplexExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        final Expression EXPRESSION3 = Expressions.isEqualTo("name", "Alice");
        final Expression COMPLEX_EXPRESSION = Expressions.or(
            Expressions.and(EXPRESSION1, EXPRESSION2),
            EXPRESSION3
        );
        assertExpressionEquals("((age > 30 AND status = 'ACTIVE') OR name = 'Alice')", COMPLEX_EXPRESSION);
    }

    @Test void hashCodeTest() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isGreaterThan("age", 30);
        assertEquals(EXPRESSION1.hashCode(), EXPRESSION2.hashCode());
    }
}