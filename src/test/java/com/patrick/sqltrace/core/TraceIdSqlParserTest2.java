package com.patrick.sqltrace.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceIdSqlParser的补充测试类
 * 覆盖更多SELECT语句的格式和场景
 */
public class TraceIdSqlParserTest2 {
    private static final Logger logger = LoggerFactory.getLogger(TraceIdSqlParserTest2.class);

    private TraceIdSqlParser parser;
    private final String testTraceId = "test-trace-id-1234567";

    @BeforeEach
    public void setup() {
        parser = new TraceIdSqlParser(true, true, "xTraceId", 1000);
    }

    // 测试 SELECT * 格式
    @ParameterizedTest(name = "测试SELECT * 格式 - {0}")
    @ValueSource(strings = {
        "SELECT * FROM users",
        "SELECT * FROM users WHERE id > 100",
        "SELECT * FROM users ORDER BY id",
        "SELECT * FROM users LIMIT 10",
        "SELECT * FROM users WHERE id > 100 ORDER BY name LIMIT 10"
    })
    public void testSelectAllFormat(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试SELECT * - 原始SQL: {}", sql);
        logger.info("测试SELECT * - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试各种COUNT格式
    @ParameterizedTest(name = "测试COUNT格式 - {0}")
    @ValueSource(strings = {
        "SELECT COUNT(*) FROM users",
        "SELECT COUNT(id) FROM users",
        "SELECT COUNT(DISTINCT id) FROM users",
        "SELECT COUNT(1) FROM users",
        "SELECT COUNT(*) as total FROM users",
        "SELECT COUNT(*), department FROM users GROUP BY department"
    })
    public void testCountFormats(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试COUNT - 原始SQL: {}", sql);
        logger.info("测试COUNT - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试子查询中的SELECT *
    @ParameterizedTest(name = "测试子查询SELECT * - {0}")
    @ValueSource(strings = {
        "SELECT * FROM (SELECT * FROM users) AS u",
        "SELECT * FROM users WHERE id IN (SELECT * FROM temp_users)",
        "SELECT * FROM users u1 WHERE EXISTS (SELECT * FROM users u2 WHERE u2.id = u1.id)",
        "SELECT * FROM (SELECT * FROM users ORDER BY id LIMIT 10) AS u"
    })
    public void testSubqueryWithSelectAll(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试子查询SELECT * - 原始SQL: {}", sql);
        logger.info("测试子查询SELECT * - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试DISTINCT
    @ParameterizedTest(name = "测试DISTINCT - {0}")
    @ValueSource(strings = {
        "SELECT DISTINCT * FROM users",
        "SELECT DISTINCT id, name FROM users",
        "SELECT DISTINCT department, name, salary FROM users",
        "SELECT DISTINCT department, COUNT(*) FROM users GROUP BY department"
    })
    public void testDistinctFormats(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试DISTINCT - 原始SQL: {}", sql);
        logger.info("测试DISTINCT - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
        // 确保traceId列在DISTINCT列表的最后
        assertFalse(result.toLowerCase().contains("xtraceId".toLowerCase() + ", "));
    }

    // 测试各种JOIN
    @ParameterizedTest(name = "测试JOIN - {0}")
    @ValueSource(strings = {
        "SELECT * FROM users u JOIN orders o ON u.id = o.user_id",
        "SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id",
        "SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id",
        "SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id",
        "SELECT * FROM users u LEFT OUTER JOIN orders o ON u.id = o.user_id",
        "SELECT * FROM users u CROSS JOIN orders"
    })
    public void testJoinFormats(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试JOIN - 原始SQL: {}", sql);
        logger.info("测试JOIN - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试聚合函数
    @ParameterizedTest(name = "测试聚合函数 - {0}")
    @ValueSource(strings = {
        "SELECT MAX(*) FROM users",
        "SELECT MIN(salary) FROM users",
        "SELECT AVG(salary) FROM users",
        "SELECT SUM(salary) FROM users",
        "SELECT COUNT(*), MAX(salary), MIN(salary), AVG(salary) FROM users",
        "SELECT department, COUNT(*), MAX(salary) FROM users GROUP BY department"
    })
    public void testAggregateFunctions(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试聚合函数 - 原始SQL: {}", sql);
        logger.info("测试聚合函数 - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试UNION相关
    @ParameterizedTest(name = "测试UNION - {0}")
    @ValueSource(strings = {
        "SELECT * FROM users UNION SELECT * FROM temp_users",
        "SELECT *,name FROM users UNION SELECT *,name FROM temp_users",
        "SELECT name ,age  FROM users UNION SELECT name ,age FROM temp_users",
        "SELECT * FROM users UNION ALL SELECT * FROM temp_users",
        "SELECT * FROM users UNION DISTINCT SELECT * FROM temp_users",
        "(SELECT * FROM users) UNION (SELECT * FROM temp_users)",
        "(SELECT *,name FROM users) UNION (SELECT *,name FROM temp_users)",
        "(SELECT name FROM users) UNION (SELECT name FROM temp_users)",
        "SELECT * FROM users WHERE id < 100 UNION SELECT * FROM users WHERE id > 200"
    })
    public void testUnionFormats(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试UNION - 原始SQL: {}", sql);
        logger.info("测试UNION - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试复杂条件和函数
    @ParameterizedTest(name = "测试复杂条件和函数 - {0}")
    @ValueSource(strings = {
        "SELECT * FROM users WHERE DATE(created_at) = CURDATE()",
        "SELECT * FROM users WHERE YEAR(created_at) = 2023",
        "SELECT *, CONCAT(first_name, ' ', last_name) as full_name FROM users",
        "SELECT *, CASE WHEN salary > 10000 THEN 'High' ELSE 'Low' END as salary_grade FROM users",
        "SELECT * FROM users WHERE id IN (1, 2, 3) AND status = 'active'",
        "SELECT * FROM users WHERE created_at BETWEEN '2023-01-01' AND '2023-12-31'"
    })
    public void testComplexConditionsAndFunctions(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试复杂条件和函数 - 原始SQL: {}", sql);
        logger.info("测试复杂条件和函数 - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试窗口函数
    @ParameterizedTest(name = "测试窗口函数 - {0}")
    @ValueSource(strings = {
        "SELECT *, ROW_NUMBER() OVER (ORDER BY salary) as row_num FROM users",
        "SELECT *, RANK() OVER (PARTITION BY department ORDER BY salary) as salary_rank FROM users",
        "SELECT *, DENSE_RANK() OVER (ORDER BY salary) as dense_rank FROM users",
        "SELECT *, FIRST_VALUE(salary) OVER (PARTITION BY department ORDER BY salary) as first_salary FROM users",
        "SELECT *, LAG(salary) OVER (ORDER BY id) as prev_salary FROM users"
    })
    public void testWindowFunctions(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试窗口函数 - 原始SQL: {}", sql);
        logger.info("测试窗口函数 - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试WITH子句（CTE）
    @ParameterizedTest(name = "测试WITH子句 - {0}")
    @ValueSource(strings = {
        "WITH cte AS (SELECT * FROM users) SELECT * FROM cte",
        "WITH cte AS (SELECT * FROM users) SELECT * FROM cte WHERE id > 100",
        "WITH cte1 AS (SELECT * FROM users), cte2 AS (SELECT * FROM orders) SELECT * FROM cte1 JOIN cte2 ON cte1.id = cte2.user_id",
        "WITH RECURSIVE cte AS (SELECT * FROM users WHERE id = 1 UNION ALL SELECT * FROM users WHERE id > 1) SELECT * FROM cte"
    })
    public void testWithClause(String sql) {
        String result = parser.parseAndModify(sql, testTraceId);
        logger.info("测试WITH子句 - 原始SQL: {}", sql);
        logger.info("测试WITH子句 - 结果SQL: {}", result);
        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
    }

    // 测试简单聚合查询
    @Test
    public void testSimpleAggregateQueries() {
        String[] testCases = {
            // 简单聚合查询 - 不应该添加traceId
            "SELECT COUNT(*) FROM users",
            "SELECT COUNT(1) FROM users",
            "SELECT COUNT(id) FROM users",
            "SELECT MAX(salary) FROM employees",
            "SELECT MIN(age) FROM users",
            "SELECT AVG(score) FROM students",
            "SELECT SUM(amount) FROM orders",
            // 带别名的简单聚合查询 - 不应该添加traceId
            "SELECT COUNT(*) as total FROM users",
            "SELECT MAX(salary) AS highest_salary FROM employees",
            "SELECT MIN(age) as youngest FROM users"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试简单聚合查询 - 原始SQL: {}", sql);
            logger.info("测试简单聚合查询 - 结果SQL: {}", result);
            
            // 验证没有添加traceId
            assertFalse(result.contains(testTraceId), "简单聚合查询不应该添加traceId");
            assertFalse(result.toLowerCase().contains("as " + "xtraceId".toLowerCase()), 
                "简单聚合查询不应该添加traceId字段");
            // 验证SQL基本没有改变（除了可能的格式变化）
            assertEquals(sql.replaceAll("\\s+", " ").trim().toLowerCase(), 
                result.replaceAll("\\s+", " ").trim().toLowerCase(),
                "简单聚合查询的SQL不应该被修改");
        }
    }

    // 测试复杂聚合查询
    @Test
    public void testComplexAggregateQueries() {
        String[] testCases = {
            // 多列聚合查询 - 应该添加traceId
            "SELECT COUNT(*), name FROM users GROUP BY name",
            "SELECT department, COUNT(*), MAX(salary) FROM employees GROUP BY department",
            "SELECT COUNT(*), YEAR(created_at) as year FROM orders GROUP BY YEAR(created_at)",
            // 子查询中的聚合 - 应该添加traceId
            "SELECT t.*, (SELECT COUNT(*) FROM orders o WHERE o.user_id = t.id) as order_count FROM users t",
            // 混合聚合和非聚合 - 应该添加traceId
            "SELECT u.name, COUNT(o.id) as order_count, MAX(o.amount) as max_amount FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.name",
            // HAVING子句 - 应该添加traceId
            "SELECT department, COUNT(*) as emp_count FROM employees GROUP BY department HAVING COUNT(*) > 10"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试复杂聚合查询 - 原始SQL: {}", sql);
            logger.info("测试复杂聚合查询 - 结果SQL: {}", result);
            
            // 验证添加了traceId
            assertTrue(result.contains(testTraceId), "复杂聚合查询应该添加traceId");
            assertTrue(result.toLowerCase().contains("as " + "xtraceId".toLowerCase()), 
                "复杂聚合查询应该添加traceId字段");
            // 验证traceId被添加到末尾
            assertTrue(result.toLowerCase().contains("," + " '" + testTraceId.toLowerCase() + "' as xtraceId"), 
                "traceId应该被添加到SELECT列表的末尾");
        }
    }

    // 测试聚合函数的边缘情况
    @Test
    public void testAggregateEdgeCases() {
        String[] testCases = {
            // 聚合函数作为表达式的一部分 - 应该添加traceId
            "SELECT name, CASE WHEN COUNT(*) > 10 THEN 'High' ELSE 'Low' END as volume FROM orders GROUP BY name",
            // 多个聚合函数在一个表达式中 - 应该添加traceId
            "SELECT name, (COUNT(*) + SUM(amount))/2 as avg_metric FROM orders GROUP BY name",
            // 聚合函数与字符串拼接 - 应该添加traceId
            "SELECT CONCAT('Total: ', COUNT(*)) as display_count, name FROM orders GROUP BY name",
            // 子查询中的简单聚合 - 外层应该添加traceId
            "SELECT name, (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) as order_count FROM users",
            // UNION中的聚合 - 两边都应该添加traceId
            "SELECT name, COUNT(*) as count FROM orders GROUP BY name UNION SELECT name, COUNT(*) as count FROM returns GROUP BY name"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试聚合边缘情况 - 原始SQL: {}", sql);
            logger.info("测试聚合边缘情况 - 结果SQL: {}", result);
            
            // 验证添加了traceId
            assertTrue(result.contains(testTraceId), "聚合边缘情况应该添加traceId");
            assertTrue(result.toLowerCase().contains("as " + "xtraceId".toLowerCase()), 
                "聚合边缘情况应该添加traceId字段");
        }
    }

    @Test
    public void testAllAggregateFunctionQueries() {
        String[] testCases = {
            // 全聚合函数查询 - 不应该添加traceId
            "SELECT COUNT(*), MAX(salary), MIN(salary), AVG(salary) FROM users",
            "SELECT COUNT(id), SUM(amount), AVG(price) FROM orders",
            "SELECT COUNT(*), MAX(score), MIN(score), AVG(score), SUM(score) FROM exams",
            // 带别名的全聚合函数查询 - 不应该添加traceId
            "SELECT COUNT(*) as total_count, MAX(salary) as highest, MIN(salary) as lowest FROM employees",
            // 带括号和复杂表达式的全聚合函数查询 - 不应该添加traceId
            "SELECT COUNT(*), MAX(price * quantity), MIN(price / 2), AVG(price + tax) FROM orders",
            // 带HAVING子句的全聚合函数查询 - 不应该添加traceId
            "SELECT COUNT(*), MAX(salary) FROM employees HAVING MAX(salary) > 10000"
        };

        for (String sql : testCases) {
            String result = parser.parseAndModify(sql, testTraceId);
            logger.info("测试全聚合函数查询 - 原始SQL: {}", sql);
            logger.info("测试全聚合函数查询 - 结果SQL: {}", result);
            
            // 验证没有添加traceId
            assertFalse(result.contains(testTraceId), 
                "全聚合函数查询不应该添加traceId: " + sql);
            assertFalse(result.toLowerCase().contains("as " + "xtraceId".toLowerCase()), 
                "全聚合函数查询不应该添加traceId字段: " + sql);
            // 验证SQL基本没有改变（除了可能的格式变化）
            assertEquals(sql.replaceAll("\\s+", " ").trim().toLowerCase(), 
                result.replaceAll("\\s+", " ").trim().toLowerCase(),
                "全聚合函数查询的SQL不应该被修改: " + sql);
        }
    }
}