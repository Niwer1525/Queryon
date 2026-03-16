# Queryon a pure Java SQL parser and query builder

Query on is a pure Java SQL parser and query builder. It's built on top of and bundled with SQLite.
The objective of this project is to provide a simple and intuitive API to build SQL queries in Java, without the need to write raw SQL strings. It also provides a way to convert queries results into Java objects.

## Features
- Support for SELECT, INSERT, UPDATE and DELETE statements
<!-- - Support for JOINs, subqueries, and common table expressions (CTEs) -->
- Support for parameterized queries
- Support for converting query results into Java objects using a simple mapping mechanism
- Built on top of SQLite, which is a lightweight and fast embedded database engine
- Pure Java implementation, no native dependencies (except for SQLite)

## Unsuported SQL features
- Views
- Triggers
- Stored procedures
- Window functions

## How to install

## How to use

First of all, if you want a full Java break down, go in the src/test/java/GlobalTest Class.

### Basics

### Expressions
Expression is the base class for all expressions in Queryon. It can be used to build complex expressions using a fluent API. For example, you can build a simple expression like this:
```java
Expression ageGreaterThan18 = Expression.of("age").isGreaterThan(18);
```
That correspond to the SQL expression `age > 18`. You can also combine expressions using logical operators like AND and OR:
```java
Expression ageGreaterThan18AndNameStartsWithA = ageGreaterThan18.and(Expression.of("name").like("A%"));
```
That correspond to the SQL expression `age > 18 AND name LIKE 'A%'`.

Here a few more complex examples :
```java
Expression complexExpression = Expression.of("age").isGreaterThan(18)
    .and(Expression.of("name").like("A%"))
    .or(Expression.of("city").isEqualTo("New York"));
```
This correspond to the SQL expression `((age > 18 AND name LIKE 'A%') OR city = 'New York')`.

```java
Expression isGreaterThan = Expression.of("age").isGreaterThan(18);
Expression startWith = Expression.of("name").like("A%");

Expression isGreaterThanAlias = Expression.of("age").isGreaterThan(25);
Expression startWithAlias = Expression.of("name").like("B%");


isGreaterThan.and(startWith).or(isGreaterThanAlias).or(startWithAlias);
```
This correspond to the SQL expression `((age > 18 AND name LIKE 'A%') OR age > 25 OR name LIKE 'B%')`.

```java
Expression isGreaterThan = Expression.of("age").isGreaterThan(18);
Expression startWith = Expression.of("name").like("A%");

Expression isGreaterThanAlias = Expression.of("age").isGreaterThan(25);
Expression startWithAlias = Expression.of("name").like("B%");


isGreaterThan.and(startWith).or(isGreaterThanAlias.and(startWithAlias));
```
This correspond to the SQL expression `((age > 18 AND name LIKE 'A%') OR (age > 25 AND name LIKE 'B%'))`.

### Tables
To create a table you need to create a class that extends the Table class. For example:
```java
public class UserTable extends Table {
    public UserTable(DataBase dataBase) {
        super(dataBase); // Call the parent constructor with the database

        this.addColumns( // Define the columns of the table
            createColumn(db, "id", EnumColumnTypes.INT).primaryKey().autoIncrement(), // Create an "id" column of type INT, set it as primary key and auto increment
            createColumn(db, "name", EnumColumnTypes.VARCHAR, 255).notNull(), // Create a "name" column of type VARCHAR with a maximum length of 255
            createColumn(db, "bio", EnumColumnTypes.TEXT), // Create a "bio" column of type TEXT
            createColumn(db, "age", EnumColumnTypes.INT), // Create an "age" column of type INT
            createColumn(db, "money", EnumColumnTypes.INT).defaultValue(250, Expression.of("money").greaterThanOrEqualTo(0)), // Create a "money" column of type INT with a default value of 250 and a CHECK constraint that money must be greater than or equal to 0
            createColumn(db, "example", ExampleEnum.class) // Create an "example" column of type ExampleEnum
            createColumn(db, "department_id", ExampleEnum.INT).unique().foreignKey(DepartmentTable.class, "id", EnumForeginKeyAction.CASCADE)
        )
        .addCheckConstraints( // Define CHECK constraints for the table
            Expression.of("age").greaterThanOrEqualTo(0) // Age must be non-negative
        )
        .execute(); // Execute the table creation in the database
    }

    @Override
    public String name() { return "users"; }

    private static enum ExampleEnum {
        VALUE1,
        VALUE2,
        VALUE3
    }
}
```
In SQL this correspond to :
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    age INT,
    money INT DEFAULT 250 CHECK (money >= 0),
    department_id INT UNIQUE,
    CHECK (age >= 0),
    FOREIGN KEY (department_id) REFERENCES department_table(id) ON DELETE CASCADE
);
```

### Insert

### Select

### Update

### Delete