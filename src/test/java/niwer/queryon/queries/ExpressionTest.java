package niwer.queryon.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ExpressionTest {

    /**
     * Helper method to assert that the generated SQL and parameters of an Expression match the expected values.
     * 
     * @param expectedSql
     * @param expectedParams
     * @param expression
     */
    private static void assertExpressionEquals(String expectedSql, List<Object> expectedParams, Expression expression) {
        assertEquals(expectedSql, expression.toString(), "The generated SQL is incorrect");
        assertEquals(expectedParams, expression.parameters(), "The stored parameters are incorrect");
    }

    /**
     * Helper method to assert that the generated SQL of an Expression matches the expected value, with no parameters.
     * e.g : IS NULL, IS NOT NULL
     * 
     * @param expectedSql
     * @param expression
     */
    private static void assertExpressionEquals(String expectedSql, Expression expression) {
        assertExpressionEquals(expectedSql, List.of(), expression);
    }

    @Test void testEquals() {
        Expression exp1 = new Expression("age > ?", 30);
        Expression exp2 = new Expression("age > ?", 30);
        assertTrue(exp1.equals(exp1), "An expression should be equal to itself");
        assertTrue(exp1.equals(exp2), "Expressions with the same SQL and parameters should be equal");
        assertTrue(exp1.hashCode() == exp2.hashCode(), "Hash codes of equal expressions should be the same");
        assertFalse(exp1.equals(null));
        assertFalse(exp1.equals("some string"));
    }

    @Test void testLogicalAndOrAndAdditionalPredicates() {
        final Expression AGE = Expressions.between("age", 18, 65);
        final Expression NAME = Expressions.like("name", "A%");

        assertExpressionEquals("(age BETWEEN ? AND ? AND name LIKE ?)",
            List.of(18, 65, "A%"), Expressions.and(AGE, NAME));
        assertExpressionEquals("(age BETWEEN ? AND ? OR name LIKE ?)",
            List.of(18, 65, "A%"), Expressions.or(AGE, NAME));
        assertEquals(AGE, Expressions.and(AGE));
        assertEquals(NAME, Expressions.or(NAME));
    }

    @Test void testInvalidExpressionArguments() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.isNull(""));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.like("name", null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.between("age", null, 10));
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status"));
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status", (Object[]) null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.and());
        assertThrows(IllegalArgumentException.class, () -> Expressions.or((Expression[]) null));
    }

    @Test void createGreaterThanExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThan("age", 30);
        assertExpressionEquals("age > ?", List.of(30), EXPRESSION);

        final Expression EXPRESSION_DATE = Expressions.isGreaterThan("created_at", Date.valueOf("2024-01-01"));
        assertExpressionEquals("created_at > ?", List.of("2024-01-01"), EXPRESSION_DATE);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThan("age", (Number)null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThan("age", (Date)null));
    }

    @Test void createLocalDateExpressions() {
        final LocalDate DATE = LocalDate.of(2024, 1, 1);
        final LocalDateTime DATE_TIME = LocalDateTime.of(2024, 1, 1, 12, 30, 45);

        assertExpressionEquals("created_at > ?", List.of("2024-01-01"), Expressions.isGreaterThan("created_at", DATE));
        assertExpressionEquals("created_at >= ?", List.of("2024-01-01"), Expressions.isGreaterThanOrEqualTo("created_at", DATE));
        assertExpressionEquals("created_at < ?", List.of("2024-01-01"), Expressions.isLessThan("created_at", DATE));
        assertExpressionEquals("created_at <= ?", List.of("2024-01-01"), Expressions.isLessThanOrEqualTo("created_at", DATE));
        assertExpressionEquals("created_at <= ?", List.of("2024-01-01 12:30:45"), Expressions.isLessThanOrEqualTo("created_at", DATE_TIME));

        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThan("created_at", (LocalDate)null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThan("created_at", (LocalDateTime)null));
    }

    @Test void createGreaterThanOrEqualToExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThanOrEqualTo("age", 30);
        assertExpressionEquals("age >= ?", List.of(30), EXPRESSION);

        final Expression EXPRESSION_DATE = Expressions.isGreaterThanOrEqualTo("created_at", Date.valueOf("2024-01-01"));
        assertExpressionEquals("created_at >= ?", List.of("2024-01-01"), EXPRESSION_DATE);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThanOrEqualTo("age", (Number)null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isGreaterThanOrEqualTo("age", (Date)null));
    }

    @Test void createLessThanExpression() {
        final Expression EXPRESSION = Expressions.isLessThan("age", 30);
        assertExpressionEquals("age < ?", List.of(30), EXPRESSION);

        final Expression EXPRESSION_DATE = Expressions.isLessThan("created_at", Date.valueOf("2024-01-01"));
        assertExpressionEquals("created_at < ?", List.of("2024-01-01"), EXPRESSION_DATE);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThan("age", (Number)null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThan("age", (Date)null));
    }

    @Test void createLessThanOrEqualToExpression() {
        final Expression EXPRESSION = Expressions.isLessThanOrEqualTo("age", 30);
        assertExpressionEquals("age <= ?", List.of(30), EXPRESSION);

        final Expression EXPRESSION_DATE = Expressions.isLessThanOrEqualTo("created_at", Date.valueOf("2024-01-01"));
        assertExpressionEquals("created_at <= ?", List.of("2024-01-01"), EXPRESSION_DATE);

        final Expression EXPRESSION_DATE_TIME = Expressions.isLessThanOrEqualTo("created_at", new java.util.Date(0));
        assertExpressionEquals("created_at <= ?", List.of("1970-01-01 00:00:00"), EXPRESSION_DATE_TIME);

        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThanOrEqualTo("age", (Number)null));
        assertThrows(IllegalArgumentException.class, () -> Expressions.isLessThanOrEqualTo("age", (Date)null));
    }

    @Test void createEqualToExpression() {
        final Expression EXPRESSION = Expressions.isEqualTo("name", "Alice");
        assertExpressionEquals("name = ?", List.of("Alice"), EXPRESSION);

        final Expression EXPRESSION_NULL = Expressions.isEqualTo("name", null);
        assertExpressionEquals("name IS NULL", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expressions.isEqualTo("name", 25);
        assertExpressionEquals("name = ?", List.of(25), EXPRESSION_OTHER);
    }

    @Test void createNotEqualToExpression() {
        final Expression EXPRESSION = Expressions.isNotEqualTo("name", "Alice");
        assertExpressionEquals("name <> ?", List.of("Alice"), EXPRESSION);

        final Expression EXPRESSION_NULL = Expressions.isNotEqualTo("name", null);
        assertExpressionEquals("name IS NOT NULL", EXPRESSION_NULL);

        final Expression EXPRESSION_OTHER = Expressions.isNotEqualTo("name", 25);
        assertExpressionEquals("name <> ?", List.of(25), EXPRESSION_OTHER);
    }

    @Test void createLocalDateEqualityExpressions() {
        assertExpressionEquals("created_at = ?", List.of("2024-01-01"),
            Expressions.isEqualTo("created_at", LocalDate.of(2024, 1, 1)));
        
        assertExpressionEquals("created_at <> ?", List.of("2024-01-01 12:30:00"),
            Expressions.isNotEqualTo("created_at", LocalDateTime.of(2024, 1, 1, 12, 30)));
        
        assertExpressionEquals("created_at IN (?, ?)", List.of("2024-01-01", "2024-01-02"),
            Expressions.in("created_at", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)));
    }

    @Test void createExpression() {
        final Expression EXPRESSION = Expressions.isGreaterThan("age", 30);
        assertExpressionEquals("age > ?", List.of(30), EXPRESSION);
    }

    @Test void createStringExpression() {
        final Expression EXPRESSION = Expressions.isEqualTo("name", "Alice");
        assertExpressionEquals("name = ?", List.of("Alice"), EXPRESSION);
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
        assertExpressionEquals("status IN (?, ?, ?)", List.of("ACTIVE", "INACTIVE", "PENDING"), EXPRESSION);
    }

    @Test void createInExpressionWithNullEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status", (Class<? extends Enum<?>>)null));
    }

    @Test void createInExpressionWithEmptyEnum() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("status", EmptyEnum.class));
    }

    private enum EmptyEnum {}

    private enum TestStatus { ACTIVE, INACTIVE, PENDING }

    @Test void createInExpressionWithValues() {
        final Expression EXPRESSION = Expressions.in("id", 1, 2, 3);
        assertExpressionEquals("id IN (?, ?, ?)", List.of(1, 2, 3), EXPRESSION);
    }

    @Test void createInExpressionWithStringValues() {
        final Expression EXPRESSION = Expressions.in("name", "Alice", "Bob", "Charlie");
        assertExpressionEquals("name IN (?, ?, ?)", List.of("Alice", "Bob", "Charlie"), EXPRESSION);
    }

    @Test void createInExpressionWithNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("id", (Object[])null));
    }

    @Test void createInExpressionWithEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.in("id"));
    }

    @Test void createInExpressionWithMixedValues() {
        Expression expression = Expressions.in("id", 1, "two", 3);
        assertExpressionEquals("id IN (?, ?, ?)", List.of(1, "two", 3), expression);
    }

    @Test void createInExpressionWithMixedStringValues() {
        final Expression EXPRESSION = Expressions.in("name", "Alice", null, "Charlie");
        // Notre logique gère les NULL dans le IN en injectant directement NULL dans la chaîne
        assertExpressionEquals("name IN (?, NULL, ?)", List.of("Alice", "Charlie"), EXPRESSION);
    }

    @Test void testLikeExpressionWithNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.like("name", null));
    }

    @Test void testLikeExpression() {
        final Expression EXPRESSION = Expressions.like("name", "A%");
        assertExpressionEquals("name LIKE ?", List.of("A%"), EXPRESSION);

        final Expression EXPRESSION2 = Expressions.like("name", "%son");
        assertExpressionEquals("name LIKE ?", List.of("%son"), EXPRESSION2);

        final Expression EXPRESSION3 = Expressions.like("name", "%ann%");
        assertExpressionEquals("name LIKE ?", List.of("%ann%"), EXPRESSION3);
    }

    @Test void testBetweenExpression() {
        final Expression EXPRESSION3 = Expressions.between("name", 25, 35);
        assertExpressionEquals("name BETWEEN ? AND ?", List.of(25, 35), EXPRESSION3);
    }

    @Test void testBetweenExpressionWithNullValues() {
        assertThrows(IllegalArgumentException.class, () -> Expressions.between("name", null, 35));
        assertThrows(IllegalArgumentException.class, () -> Expressions.between("name", 25, null));
    }

    @Test void testAndExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        assertExpressionEquals("(age > ? AND status = ?)", List.of(30, "ACTIVE"), Expressions.and(EXPRESSION1, EXPRESSION2));
    }

    @Test void testOrExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        assertExpressionEquals("(age > ? OR status = ?)", List.of(30, "ACTIVE"), Expressions.or(EXPRESSION1, EXPRESSION2));
    }

    @Test void testComplexExpression() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isEqualTo("status", "ACTIVE");
        final Expression EXPRESSION3 = Expressions.isEqualTo("name", "Alice");
        final Expression COMPLEX_EXPRESSION = Expressions.or(
            Expressions.and(EXPRESSION1, EXPRESSION2),
            EXPRESSION3
        );
        assertExpressionEquals("((age > ? AND status = ?) OR name = ?)", List.of(30, "ACTIVE", "Alice"), COMPLEX_EXPRESSION);
    }

    @Test void hashCodeTest() {
        final Expression EXPRESSION1 = Expressions.isGreaterThan("age", 30);
        final Expression EXPRESSION2 = Expressions.isGreaterThan("age", 30);
        assertEquals(EXPRESSION1.hashCode(), EXPRESSION2.hashCode());
    }
}