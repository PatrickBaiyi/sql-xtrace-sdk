package com.patrick.sqltrace.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.patrick.sqltrace.core.TraceIdSqlParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceIdSqlParser的补充测试类
 * 专门用于测试非SELECT语句（UPDATE、INSERT、DELETE）的场景
 */
public class TraceIdSqlParserTest3 {
    private static final Logger logger = LoggerFactory.getLogger(TraceIdSqlParserTest3.class);

    private TraceIdSqlParser parser;
    private final String testTraceId = "test-trace-id-1234567";

    @BeforeEach
    public void setup() {
        parser = new TraceIdSqlParser(true, true, "xTraceId", 1000);
    }

    // 测试基本的INSERT语句
    @ParameterizedTest(name = "测试基本INSERT - {0}")
    @ValueSource(strings = {
        "INSERT INTO users (name, age) VALUES ('John', 25)",
        "INSERT INTO users SET name = 'John', age = 25",
        "INSERT INTO users VALUES ('John', 25, 'john@example.com')",
        "INSERT IGNORE INTO users (name, age) VALUES ('John', 25)",
        "INSERT INTO users (name, age) VALUES ('John', 25) ON DUPLICATE KEY UPDATE age = 25"
    })
    public void testBasicInsert(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试基本INSERT - 原始SQL: {}", sql);
        logger.info("测试基本INSERT - 结果SQL: {}", result);
        
        // INSERT语句不应该被修改
        assertEquals(sql.replaceAll("\\s+", " ").trim(), 
            result.replaceAll("\\s+", " ").trim(),
            "INSERT语句不应该被修改");
    }

    // 测试带子查询的INSERT语句
    @Test
    public void testInsertWithSubquery() {
        String[] testCases = {
            "INSERT INTO active_users SELECT * FROM users WHERE status = 'active'",
            "INSERT INTO employee_summary SELECT department, COUNT(*) FROM employees GROUP BY department",
            "INSERT INTO high_salary_employees SELECT * FROM employees WHERE salary > (SELECT AVG(salary) FROM employees)",
            "INSERT INTO department_stats (dept_name, emp_count) SELECT department, COUNT(*) FROM employees GROUP BY department",
            "INSERT INTO user_stats SELECT id, (SELECT COUNT(*) FROM orders WHERE user_id = users.id) FROM users"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试带子查询的INSERT - 原始SQL: {}", sql);
            logger.info("测试带子查询的INSERT - 结果SQL: {}", result);
            
            // INSERT语句不应该被修改，即使包含SELECT子查询
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "带子查询的INSERT语句不应该被修改");
        }
    }

    // 测试基本的UPDATE语句
    @ParameterizedTest(name = "测试基本UPDATE - {0}")
    @ValueSource(strings = {
        "UPDATE users SET name = 'John' WHERE id = 1",
        "UPDATE users SET age = age + 1",
        "UPDATE users SET status = 'inactive' WHERE last_login < DATE_SUB(NOW(), INTERVAL 1 YEAR)",
        "UPDATE users SET name = 'John', age = 25 WHERE id IN (1, 2, 3)",
        "UPDATE IGNORE users SET status = 'active' WHERE department = 'IT'"
    })
    public void testBasicUpdate(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试基本UPDATE - 原始SQL: {}", sql);
        logger.info("测试基本UPDATE - 结果SQL: {}", result);
        
        // UPDATE语句不应该被修改
        assertEquals(sql.replaceAll("\\s+", " ").trim(), 
            result.replaceAll("\\s+", " ").trim(),
            "UPDATE语句不应该被修改");
    }

    // 测试带子查询的UPDATE语句
    @Test
    public void testUpdateWithSubquery() {
        String[] testCases = {
            "UPDATE users SET salary = (SELECT AVG(salary) FROM employees) WHERE department = 'IT'",
            "UPDATE employees SET bonus = salary * 0.1 WHERE department IN (SELECT dept_name FROM departments WHERE region = 'APAC')",
            "UPDATE orders SET status = 'completed' WHERE id IN (SELECT order_id FROM payments WHERE status = 'success')",
            "UPDATE products SET price = price * 1.1 WHERE category_id = (SELECT id FROM categories WHERE name = 'Electronics')",
            "UPDATE employees e SET salary = (SELECT AVG(salary) FROM employees WHERE department = e.department)"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试带子查询的UPDATE - 原始SQL: {}", sql);
            logger.info("测试带子查询的UPDATE - 结果SQL: {}", result);
            
            // UPDATE语句不应该被修改，即使包含SELECT子查询
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "带子查询的UPDATE语句不应该被修改");
        }
    }

    // 测试基本的DELETE语句
    @ParameterizedTest(name = "测试基本DELETE - {0}")
    @ValueSource(strings = {
        "DELETE FROM users WHERE id = 1",
        "DELETE FROM users",
        "DELETE FROM users WHERE created_at < '2020-01-01'",
        "DELETE FROM users WHERE status = 'inactive' AND last_login < DATE_SUB(NOW(), INTERVAL 1 YEAR)",
        "DELETE IGNORE FROM users WHERE department = 'IT'"
    })
    public void testBasicDelete(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试基本DELETE - 原始SQL: {}", sql);
        logger.info("测试基本DELETE - 结果SQL: {}", result);
        
        // DELETE语句不应该被修改
        assertEquals(sql.replaceAll("\\s+", " ").trim(), 
            result.replaceAll("\\s+", " ").trim(),
            "DELETE语句不应该被修改");
    }

    // 测试带子查询的DELETE语句
    @Test
    public void testDeleteWithSubquery() {
        String[] testCases = {
            "DELETE FROM users WHERE id IN (SELECT user_id FROM inactive_accounts)",
            "DELETE FROM products WHERE category_id = (SELECT id FROM categories WHERE name = 'Discontinued')",
            "DELETE FROM orders WHERE status IN (SELECT status FROM order_status WHERE is_final = 1)",
            "DELETE FROM employees WHERE department IN (SELECT dept_name FROM departments WHERE is_active = 0)",
            "DELETE FROM users WHERE NOT EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试带子查询的DELETE - 原始SQL: {}", sql);
            logger.info("测试带子查询的DELETE - 结果SQL: {}", result);
            
            // DELETE语句不应该被修改，即使包含SELECT子查询
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "带子查询的DELETE语句不应该被修改");
        }
    }

    // 测试多表UPDATE语句
    @Test
    public void testMultiTableUpdate() {
        String[] testCases = {
            "UPDATE users u JOIN orders o ON u.id = o.user_id SET u.total_orders = u.total_orders + 1",
            "UPDATE employees e, departments d SET e.salary = e.salary * 1.1 WHERE e.dept_id = d.id AND d.name = 'IT'",
            "UPDATE products p LEFT JOIN inventory i ON p.id = i.product_id SET p.in_stock = i.quantity > 0",
            "UPDATE orders o, users u, products p SET o.status = 'ready' WHERE o.user_id = u.id AND o.product_id = p.id",
            "UPDATE employees e JOIN salaries s ON e.id = s.employee_id SET e.salary = s.amount WHERE s.is_current = 1"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试多表UPDATE - 原始SQL: {}", sql);
            logger.info("测试多表UPDATE - 结果SQL: {}", result);
            
            // 多表UPDATE语句不应该被修改
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "多表UPDATE语句不应该被修改");
        }
    }

    // 测试多表DELETE语句
    @Test
    public void testMultiTableDelete() {
        String[] testCases = {
            "DELETE u FROM users u JOIN inactive_accounts ia ON u.id = ia.user_id",
            "DELETE e, d FROM employees e JOIN departments d ON e.dept_id = d.id WHERE d.is_active = 0",
            "DELETE p FROM products p LEFT JOIN inventory i ON p.id = i.product_id WHERE i.quantity = 0",
            "DELETE o, oi FROM orders o JOIN order_items oi ON o.id = oi.order_id WHERE o.status = 'cancelled'",
            "DELETE u, o FROM users u LEFT JOIN orders o ON u.id = o.user_id WHERE u.last_login < '2020-01-01'"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试多表DELETE - 原始SQL: {}", sql);
            logger.info("测试多表DELETE - 结果SQL: {}", result);
            
            // 多表DELETE语句不应该被修改
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "多表DELETE语句不应该被修改");
        }
    }

    // 测试批量INSERT语句
    @Test
    public void testBulkInsert() {
        String[] testCases = {
            "INSERT INTO users (name, age) VALUES ('John', 25), ('Jane', 30), ('Bob', 35)",
            "INSERT IGNORE INTO logs (event, timestamp) VALUES ('login', NOW()), ('logout', NOW())",
            "INSERT INTO employees (name, department) SELECT name, 'IT' FROM candidates WHERE score > 80 UNION SELECT name, 'HR' FROM referrals",
            "INSERT INTO user_points (user_id, points) VALUES (1, 100), (2, 200) ON DUPLICATE KEY UPDATE points = VALUES(points)",
            "INSERT INTO products (name, price) VALUES ('Product A', 10.00), ('Product B', 20.00), ('Product C', 30.00) ON DUPLICATE KEY UPDATE price = VALUES(price)"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试批量INSERT - 原始SQL: {}", sql);
            logger.info("测试批量INSERT - 结果SQL: {}", result);
            
            // 批量INSERT语句不应该被修改
            assertEquals(sql.replaceAll("\\s+", " ").trim(), 
                result.replaceAll("\\s+", " ").trim(),
                "批量INSERT语句不应该被修改");
        }
    }
} 