/* ------------------------------------------------------------- */
/* 2) INSERT - all common forms                                 */
/* ------------------------------------------------------------- */

/* 2.5 UPSERT: ON CONFLICT DO NOTHING */
INSERT INTO users AS u (id, email, username, age, salary, department_id)
VALUES (4, 'dave@example.com', 'dave', 29, 65000, 3)
ON CONFLICT(email) DO NOTHING;

/* 2.6 UPSERT: ON CONFLICT DO UPDATE with excluded alias */
INSERT INTO users AS u (id, email, username, age, salary, department_id)
VALUES (4, 'dave@example.com', 'dave-updated', 30, 68000, 1)
ON CONFLICT(email) DO UPDATE SET
    username = excluded.username,
    age = excluded.age,
    salary = excluded.salary,
    department_id = excluded.department_id
WHERE u.salary < excluded.salary;

/* 2.7 INSERT INTO ... SELECT */
INSERT INTO users (id, email, username, age, salary, department_id)
SELECT
    5,
    'eve@example.com',
    'eve',
    CAST(AVG(age) AS INTEGER),
    60000,
    1
FROM users
WHERE department_id = 1;

/* ------------------------------------------------------------- */
/* 3) SELECT - projections, filters, joins, groups, CTE, window */
/* ------------------------------------------------------------- */

/* 3.4 Expressions and CASE */
SELECT
    u.id,
    u.username,
    u.salary,
    CASE
        WHEN u.salary >= 100000 THEN 'SENIOR'
        WHEN u.salary >= 60000 THEN 'MID'
        ELSE 'JUNIOR'
    END AS level
FROM users u;

/* 3.5 Aggregations */
SELECT COUNT(*) AS total_users FROM users;
SELECT department_id, COUNT(*) AS n_users, AVG(salary) AS avg_salary
FROM users
GROUP BY department_id
HAVING COUNT(*) >= 1;

/* 3.6 Joins */
SELECT u.username, d.name AS department_name
FROM users u
INNER JOIN departments d ON d.id = u.department_id;

SELECT u.username, m.username AS manager_name
FROM users u
LEFT JOIN users m ON m.id = u.manager_id;

SELECT d.code, p.category
FROM departments d
CROSS JOIN (SELECT DISTINCT category FROM products) p;

/* 3.7 Subqueries */
SELECT *
FROM users
WHERE salary > (SELECT AVG(salary) FROM users);

SELECT *
FROM users u
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
);

SELECT username
FROM users
WHERE id IN (
    SELECT user_id FROM orders WHERE status = 'PENDING'
);

/* 3.8 Set operations */
SELECT username FROM users WHERE department_id = 1
UNION
SELECT username FROM users WHERE department_id = 2;

SELECT username FROM users WHERE department_id = 1
INTERSECT
SELECT username FROM users WHERE salary > 60000;

SELECT username FROM users WHERE department_id = 1
EXCEPT
SELECT username FROM users WHERE salary > 60000;

/* 3.9 CTE (non recursive) */
WITH high_paid AS (
    SELECT id, username, salary
    FROM users
    WHERE salary >= 65000
)
SELECT * FROM high_paid ORDER BY salary DESC;

/* 3.10 Recursive CTE */
WITH RECURSIVE numbers(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 5
)
SELECT n FROM numbers;

/* 3.11 Window functions */
SELECT
    u.id,
    u.username,
    u.salary,
    ROW_NUMBER() OVER (ORDER BY u.salary DESC) AS row_num,
    RANK() OVER (ORDER BY u.salary DESC) AS salary_rank,
    SUM(u.salary) OVER (PARTITION BY u.department_id) AS department_salary_sum
FROM users u;

/* ------------------------------------------------------------- */
/* 4) UPDATE - simple, expression, correlated, from, returning  */
/* ------------------------------------------------------------- */

/* 4.1 Basic update */
UPDATE users
SET age = 31
WHERE id = 1;

/* 4.2 Update multiple columns with expressions */
UPDATE users
SET
    salary = salary * 1.05,
    updated_at = CURRENT_TIMESTAMP
WHERE department_id = 1;

/* 4.3 Correlated update via subquery */
UPDATE orders
SET total_amount = (
    SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
    FROM order_items oi
    WHERE oi.order_id = orders.id
)
WHERE id = 1000;

/* 4.4 UPDATE ... FROM (SQLite >= 3.33) */
UPDATE users
SET salary = salary + bonus.extra
FROM (
    SELECT 1 AS user_id, 1000 AS extra
    UNION ALL
    SELECT 2 AS user_id, 500 AS extra
) AS bonus
WHERE users.id = bonus.user_id;

/* ------------------------------------------------------------- */
/* 6) VIEW / INDEX / TRIGGER                                    */
/* ------------------------------------------------------------- */

CREATE VIEW v_active_users AS
SELECT
    u.id,
    u.username,
    u.email,
    d.name AS department_name
FROM users u
LEFT JOIN departments d ON d.id = u.department_id
WHERE u.is_active = 1;

SELECT * FROM v_active_users;

CREATE INDEX idx_users_department_salary ON users(department_id, salary);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

CREATE TRIGGER trg_users_updated_at
AFTER UPDATE OF salary ON users
FOR EACH ROW
WHEN OLD.salary <> NEW.salary
BEGIN
    UPDATE users
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;

    INSERT INTO user_audit (user_id, old_salary, new_salary)
    VALUES (NEW.id, OLD.salary, NEW.salary);
END;

UPDATE users SET salary = salary + 250 WHERE id = 1;

/* ------------------------------------------------------------- */
/* 7) Transactions and savepoints                               */
/* ------------------------------------------------------------- */

BEGIN TRANSACTION;

INSERT INTO orders (id, user_id, status) VALUES (1001, 1, 'PENDING');
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
VALUES (1001, 103, 10, 5);

SAVEPOINT after_first_item;

/* Simulate a branch where we rollback only part of work */
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
VALUES (1001, 102, 1, 75);

ROLLBACK TO after_first_item;
RELEASE after_first_item;

UPDATE orders
SET total_amount = (
    SELECT COALESCE(SUM(quantity * unit_price), 0)
    FROM order_items
    WHERE order_id = 1001
)
WHERE id = 1001;

COMMIT;