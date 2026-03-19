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
- Transactions
- Select JOINS, GROUP BY and HAVING
- Select COUNT, SUM, AVG, MIN, MAX and other aggregate functions

## How to install

## How to use

First of all, if you want a full Java break down, go in the src/test/java/GlobalTest Class.

### Basics
First of all, you have to create a DataBase object to connect to your database. For example:
```java
final DataBase DATA_BASE = new DataBase(new File("path/to/database.db"));
```

If you want, you can force connect, disconnect or reconnect to the database using the following methods :
```java
DATA_BASE.connect();
DATA_BASE.disconnect();
DATA_BASE.reconnect();
```
But keep in mind that all the queries will automatically connect to the database if it's not already connected, so you don't need to worry about that.

The DataBase class also provides few otherfunctions :
```java
DATA_BASE#tabExist(Table table); // Check if a table exist in the database
DATA_BASE#tabExist(Class<? extends Table> tableClass); // Check if a table exist in the database
DATA_BASE#dropTable(Class<? extends Table> tableClass); // Drop a table from the database
DATA_BASE#getTable(Class<? extends Table> tableClass); // Get a table from the database
```

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

Here's the list of all the supported expressions :
```java
final Expression EXPRESSION = Expression.of("age");
EXPRESSION.isNull();
EXPRESSION.isNotNull();
EXPRESSION.isGreaterThan(30);
EXPRESSION.isGreaterThanOrEqualTo(30);
EXPRESSION.isLessThan(30);
EXPRESSION.isLessThanOrEqualTo(30);
EXPRESSION.isEqualTo(30);
EXPRESSION.isNotEqualTo(30);
EXPRESSION.in("Alice", "Bob");
EXPRESSION.in(TestStatus.class);
EXPRESSION.like("A%");
EXPRESSION.between(25, 35);
```

### Objects to SQL and SQL to Objects
Queryon provides a simple way to convert Java objects to SQL values and vice versa.
First you have to understand has multiple custom annotations : IColumnField and IForeignKey.
These annotations are used to define the mapping between Java fields and SQL columns. You can use them to specify the column name, type, constraints, etc.

```java
@IColumnField(
    name = "id",
    charLimit = 255, // Only for string types, specify a character limit for VARCHAR columns
    autoIncrement = true, // Only for integer types, specify if the column should be AUTO_INCREMENT
    notNull = true,
    unique = true,
    primaryKey = true
    foreignKey = @IForeignKey(table = DepartmentTable.class, column = "id", onDelete = EnumForeginKeyAction.CASCADE) // Specify a foreign key relationship (optional)
    defaultValue = @IDefaultValue(value = "250") // Specify a default value and an optional CHECK constraint for the column (optional)
)

// You can specify the "on delete" action for the foreign key using the EnumForeginKeyAction enum, which has the following values :
enum EnumForeginKeyAction {
    CASCADE,
    SET_NULL,
    SET_DEFAULT,
    NO_ACTION;
}
```

Here's a small example of a class that implements SQLSerializable and uses the IColumnField annotation to define the mapping between Java fields and SQL columns :
```java
public class User extends SQLSerializable<User> {
    @IColumnField(name = "id", autoIncrement = true, primaryKey = true)
    private int id; // INT column, primary key, auto increment

    @IColumnField(name = "name", charLimit = 255, notNull = true)
    private String name; // Varchar(255) column, not null

    @IColumnField(name = "bio")
    private String bio; // TEXT column
}
```

Note that IColumnField doesn't support check contraints. To add a check constraint to a column, you HAVE TO add it manually in the table definition using the addCheckConstraints method. For example :
```java
public class UserTable extends Table {
    public UserTable(DataBase dataBase) {
        super(dataBase); // Call the parent constructor with the database
        this.addCheckConstraints(Expression.of("age").greaterThanOrEqualTo(0)) // Define CHECK constraints for the table
    }

    @Override
    public String name() { return "users"; }
}
```

### Tables
To create a table you have two ways to do it. In both cases you'll need to create a class that extends the Table class and override the name() method to specify the name of the table in the database.

The first boring way one is to crreate columns manually. For example:
```java
public class UserTable extends Table {
    public UserTable(DataBase dataBase) {
        super(dataBase); // Call the parent constructor with the database
        
        // This is to use with caution ! In this case it's a showcase. You should avoid dropping tables in production code.
        this.dropTable(db); // Drop the table if it already exist to avoid conflicts

        this.addColumns( // Define the columns of the table
            createColumn(db, "id", EnumColumnTypes.INT).primaryKey().autoIncrement(), // Create an "id" column of type INT, set it as primary key and auto increment
            createColumn(db, "name", EnumColumnTypes.VARCHAR, 255).notNull(), // Create a "name" column of type VARCHAR with a maximum length of 255
            createColumn(db, "bio", EnumColumnTypes.TEXT), // Create a "bio" column of type TEXT
            createColumn(db, "age", EnumColumnTypes.INT), // Create an "age" column of type INT
            createColumn(db, "money", EnumColumnTypes.INT).defaultValue(250, Expression.of("money").greaterThanOrEqualTo(0)), // Create a "money" column of type INT with a default value of 250 and a CHECK constraint that money must be greater than or equal to 0
            createColumn(db, "start_date", EnumColumnTypes.DATE_TIME).defaultValue("CURRENT_TIMESTAMP"), // Create a "start_date" column of type DATE_TIME with a default value of "CURRENT_TIMESTAMP"
            createColumn(db, "end_date", EnumColumnTypes.DATE).defaultValue("0000-00-00"), // Create a "end_date" column of type DATE with a default value of "0000-00-00"
            createColumn(db, "another_date", EnumColumnTypes.DATE).defaultValue(new java.util.Date()), // Create a "another_date" column of type DATE with a default value of the current date
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

But if you have a class that implements SQLSerializable, you can create a table directly from it using the IColumnField annotation to define the columns. Creating a class that implements SQLSerializable and using IColumnField annotations is a more convenient way to create a table plus it allows you to easily convert query results into Java objects and vice versa.

For example:
```java
public class User implements SQLSerializable<User> {
    @IColumnField(name = "id", autoIncrement = true, primaryKey = true)
    private int id;

    @IColumnField(name = "name", charLimit = 255, notNull = true)
    private String name;

    @IColumnField(name = "bio")
    private String bio;

    @IColumnField(name = "age")
    private int age;

    @IColumnField(name = "money")
    private int money;

    @IColumnField(name = "start_date")
    private Date startDate = new Date(0); // Default value for start_date column, but this doesn't support Expression CHECK constraint. To add constraints you have to add them manually in the table definition using the addCheckConstraints method as shown in the previous example.

    @IColumnField(name = "end_date")
    private Date endDate;

    @IColumnField(name = "another_date")
    private Date anotherDate;

    @IColumnField(name = "example")
    private ExampleEnum example;
}

public class UserTable extends Table {
    public UserTable(DataBase dataBase) {
        super(dataBase); // Call the parent constructor with the database
        
        // This is to use with caution ! In this case it's a showcase. You should avoid dropping tables in production code.
        this.dropTable(db); // Drop the table if it already exist to avoid conflicts

        this.addColumnsFromClass(User.class).execute(); // Create columns from the User class and execute the table creation in the database
    }

    @Override
    public String name() { return "users"; }
}
```

In SQL this correspond to :
```sql
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    age INT,
    money INT DEFAULT 250 CHECK (money >= 0),
    start_date DATE_TIME DEFAULT CURRENT_TIMESTAMP,
    end_date DATE DEFAULT '0000-00-00',
    another_date DATE DEFAULT 'CONVERTED_FROM_JAVA_DATE',
    example TEXT CHECK (example IN ('VALUE1', 'VALUE2', 'VALUE3')),
    department_id INT UNIQUE,
    CHECK (age >= 0),
    FOREIGN KEY (department_id) REFERENCES department_table(id) ON DELETE CASCADE
);
```

And then to register the table you simply need to create an instance of it :
```java
final DataBase DATA_BASE = new DataBase(new File("path/to/database.db")).registerTable(UserTable.class);
```

### Insert
Insertions are pretty simple to do. You can do a simple insertion with like this :
```java
InsertionManager.insert(DB, TestUserTable.class, "id", "name", "age")
    .row(1, "Alice", 30)
    .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
    .execute();
```
This correspond to the SQL query `INSERT INTO test_table (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28);`.

Or you can do an insertion while ignoring duplicates like this :
```java
InsertionManager.insertOrIgnore(DB, TestUserTable.class, "id", "name", "age")
    .row(1, "Alice", 30)
    .rows(InsertionManager.of(2, "Bob", 25), InsertionManager.of(3, "Carol", 28))
    .execute();
```
This correspond to the SQL query `INSERT OR IGNORE INTO test_table (id, name, age) VALUES (1, 'Alice', 30), (2, 'Bob', 25), (3, 'Carol', 28);`.

Alternatively, you can setup a conflict resolution strategy for the insertion like this :
```java
InsertionManager.insertWithConflictResolution(DB, TestUserTable.class, "id", "name", "age")
    .conflictResolutionStrategy(EnumConflictResolutionStrategy.REPLACE) // This will replace the existing row with the new one if a conflict occur
    .row(1, "Alice", 30)
    .onConflicDoNothing() // This will do nothing if a conflict occur, you can also use onConflictDoUpdate() to update the existing row with the new one if a conflict occur
    .execute();
```
This correspond to the SQL query `INSERT INTO test_table (id, name, age) VALUES (1, 'Alice', 30) ON CONFLICT(id) DO NOTHING;`.

```java
InsertionManager.insertWithConflictResolution(DB, TestUserTable.class, "id", "name", "age")
    .conflictResolutionStrategy(EnumConflictResolutionStrategy.REPLACE) // This will replace the existing row with the new one if a conflict occur
    .row(1, "Alice", 30)
    .onConflictDoUpdate(
        UpdateManager.update(DB, TestUserTable.class).set("name", "Alice Updated").where(Expression.of("id").isEqualTo(1))
    )
    .execute();
```
This correspond to the SQL query `INSERT INTO test_table (id, name, age) VALUES (1, 'Alice', 30) ON CONFLICT(id) DO UPDATE SET name = 'Alice Updated' WHERE id = 1;`.

### Select
You can do a simple select like this :
```java
SelectionManager.select(DATA_BASE, UserTable.class).executeSerializable(User.class);
```
This correspond to the SQL query `SELECT * FROM users;`.

Note you can replace the `select` method with `selectDistinct` if you want to select only distinct rows like this :
```java
SelectionManager.selectDistinct(DATA_BASE, UserTable.class).executeSerializable(User.class);
```
This correspond to the SQL query `SELECT DISTINCT * FROM users;`.

You can also add a where clause to the select like this :
```java
SelectionManager.select(DATA_BASE, UserTable.class)
    .where(Expression.of("age").greaterThan(25))
    .executeSerializable(User.class);
```
This correspond to the SQL query `SELECT * FROM users WHERE age > 25;`.

You can add an order by clause to the select like this :
```java
SelectionManager.select(DATA_BASE, UserTable.class)
    .where(Expression.of("age").greaterThan(25))
    .orderBy("age", EnumOrder.DESC)
    .orderBy("name", EnumOrder.ASC)
    .executeSerializable(User.class);
```
This correspond to the SQL query `SELECT * FROM users WHERE age > 25 ORDER BY age DESC, name ASC;`.

You can also add a limit clause to the select like this :
```java
SelectionManager.select(DATA_BASE, UserTable.class)
    .where(Expression.of("age").greaterThan(25))
    .orderBy("age", EnumOrder.DESC)
    .limit(10)
    .executeSerializable(User.class);
```
This correspond to the SQL query `SELECT * FROM users WHERE age > 25 ORDER BY age DESC LIMIT 10;`.

Note that Queryon selection manager allows you to get the result as an object, a list of objects or a primitive value.
```java
SelectionManager.select(DATA_BASE, UserTable.class).executeSerializable(User.class); // Will return a User object if the query result contain only one row, otherwise it will throw an exception
SelectionManager.select(DATA_BASE, UserTable.class).executeList(User.class); // Will return a List<User> object containing all the rows of the query result, even if there's only one row or no row at all (in that case it will return an empty list)
SelectionManager.select(DATA_BASE, UserTable.class).executePrimitive(int.class); // Will return an int value if the query result contain only one row and one column, otherwise it will throw an exception
SelectionManager.select(DATA_BASE, UserTable.class).executeHasResult(); // Will return true if the query result contain at least one row, false otherwise
SelectionManager.select(DATA_BASE, UserTable.class).executeCountResults(); // Will return the count of results returned by the query, or 0 if the query returns no results or if an error occurs
```

### Update
You can do a simple update like this :
```java
UpdateManager.update(DATA_BASE, UserTable.class)
    .set("age", 31)
    .where(Expression.of("id").isEqualTo(1))
    .execute();
```
This correspond to the SQL query `UPDATE users SET age = 31 WHERE id = 1;`.

You can also update multiple columns at once like this :
```java
UpdateManager.update(DATA_BASE, UserTable.class)
    .set("age", 31)
    .set("name", "Alice Smith")
    .where(Expression.of("id").isEqualTo(1))
    .execute();
```
This correspond to the SQL query `UPDATE users SET age = 31, name = 'Alice Smith' WHERE id = 1;`.

You can also update columns using expressions like this :
```java
UpdateManager.update(DATA_BASE, UserTable.class)
    .set("age", "age + 1") // This will increment the age by 1
    .set("salary_bonus", "salary_bonus * 1.1 + 5") // This will increment the salary_bonus by 10% and then add 5
    .set("salary", "salary + salary_bonus") // This will increment the salary by the value of salary_bonus
    .where(Expression.of("id").isEqualTo(1))
    .execute();
```

You can also use subqueries in the update like this :
```java
UpdateManager.update(DATA_BASE, UserTable.class)
    .set("age", SelectionManager.select(DATA_BASE, UserTable.class)
        .where(Expression.of("id").isEqualTo(2))
    ) // This will set the age to the age of the user with id 2
    .where(Expression.of("id").isEqualTo(1))
    .execute();
```

### Delete
```java
DeletionManager.delete(DATA_BASE, UserTable.class)
    .where(Expression.of("age").lessThan(25))
    .execute();
```