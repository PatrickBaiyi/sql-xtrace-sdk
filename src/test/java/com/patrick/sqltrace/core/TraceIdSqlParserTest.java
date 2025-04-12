package com.patrick.sqltrace.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceIdSqlParser的全面单元测试类
 * 涵盖各种SQL场景和边缘情况
 */
public class TraceIdSqlParserTest {
    private static final Logger logger = LoggerFactory.getLogger(TraceIdSqlParserTest.class);

    private TraceIdSqlParser parserWithAllOptions;
    private TraceIdSqlParser parserWithoutOptions;
    private TraceIdSqlParser parserWithCustomField;
    private final String testTraceId = "test-trace-id-1234567";

    @BeforeEach
    public void setup() {
        // 创建启用所有选项的解析器
        parserWithAllOptions = new TraceIdSqlParser(true, true, "xTraceId", 1000);

        // 创建禁用COUNT和GROUP BY选项的解析器
        parserWithoutOptions = new TraceIdSqlParser(false, false, "xTraceId", 20000);

        // 创建使用自定义字段名的解析器
        parserWithCustomField = new TraceIdSqlParser(true, true, "customTraceField", 200000);
    }

      // 基础SELECT语句测试
    @Test
    public void testBasicSelectStatement0() {
        String sql = "SELECT * users WHERE status = 'active'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试基础SELECT语句 - 原始SQL: {}", sql);
        logger.info("测试基础SELECT语句 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
        assertTrue(result.toLowerCase().contains("username, email"));
    }
    
    // 基础SELECT语句测试
    @Test
    public void testBasicSelectStatement() {
        String sql = "SELECT username, email FROM users WHERE status = 'active'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试基础SELECT语句 - 原始SQL: {}", sql);
        logger.info("测试基础SELECT语句 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as xtraceId".toLowerCase()));
        assertTrue(result.toLowerCase().contains("username, email"));
    }

    // 非SELECT语句测试
    @Test
    public void testNonSelectStatement() {
        String sql = "UPDATE users SET status = 'inactive' WHERE last_login < '2023-01-01'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试非SELECT语句 - 原始SQL: {}", sql);
        logger.info("测试非SELECT语句 - 结果SQL: {}", result);

        // 应该保持不变
        assertEquals(sql, result);
    }

    // 启用COUNT查询处理的测试
    @Test
    public void testCountQueryWithOptionEnabled() {
        String sql = "SELECT COUNT(*) FROM users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试启用COUNT查询 - 原始SQL: {}", sql);
        logger.info("测试启用COUNT查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("count(*)"));
    }

    // 禁用COUNT查询处理的测试
    @Test
    public void testCountQueryWithOptionDisabled() {
        String sql = "SELECT COUNT(*) FROM users";
        String result = parserWithoutOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试禁用COUNT查询 - 原始SQL: {}", sql);
        logger.info("测试禁用COUNT查询 - 结果SQL: {}", result);

        // 应该保持不变
        assertEquals(sql, result);
    }

    // 启用GROUP BY查询处理的测试
    @Test
    public void testGroupByQueryWithOptionEnabled() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试启用GROUP BY查询 - 原始SQL: {}", sql);
        logger.info("测试启用GROUP BY查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("group by department"));
    }

    // 禁用GROUP BY查询处理的测试
    @Test
    public void testGroupByQueryWithOptionDisabled() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        String result = parserWithoutOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试禁用GROUP BY查询 - 原始SQL: {}", sql);
        logger.info("测试禁用GROUP BY查询 - 结果SQL: {}", result);

        // 应该保持不变
        assertEquals(sql, result);
    }

    // 测试自定义字段名
    @Test
    public void testCustomFieldName() {
        String sql = "SELECT username FROM users";
        String result = parserWithCustomField.parseAndModify(sql, testTraceId);
        
        logger.info("测试自定义字段名 - 原始SQL: {}", sql);
        logger.info("测试自定义字段名 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("as customtracefield"));
    }

    // 测试SQL重复处理的场景 - 验证是否会重复添加traceId
    @Test
    public void testRepeatProcessingSameSQL() {
        String sql = "SELECT username, email FROM users WHERE status = 'active'";
        
        // 第一次处理SQL
        String firstResult = parserWithAllOptions.parseAndModify(sql, testTraceId);
        logger.info("测试SQL重复处理 - 原始SQL: {}", sql);
        logger.info("测试SQL重复处理 - 第一次处理结果: {}", firstResult);
        
        // 再次处理已经修改过的SQL
        String secondResult = parserWithAllOptions.parseAndModify(firstResult, testTraceId);
        logger.info("测试SQL重复处理 - 第二次处理结果: {}", secondResult);
        
        // 检查第二次处理是否与第一次相同
        assertEquals(firstResult, secondResult, "重复处理SQL应该不会添加重复的traceId列");
        
        // 确保只有一个traceId列
        int traceIdCount = countOccurrences(secondResult.toLowerCase(), "as xtraceId".toLowerCase());
        assertEquals(1, traceIdCount, "SQL中应该只有一个traceId列");
    }
    
    // 测试不同traceId值的重复处理
    @Test
    public void testRepeatProcessingWithDifferentTraceId() {
        String sql = "SELECT username, email FROM users WHERE status = 'active'";
        String anotherTraceId = "another-trace-id-789012";
        
        // 第一次处理SQL
        String firstResult = parserWithAllOptions.parseAndModify(sql, testTraceId);
        logger.info("测试不同traceId值重复处理 - 原始SQL: {}", sql);
        logger.info("测试不同traceId值重复处理 - 第一次处理结果 (traceId={}): {}", testTraceId, firstResult);
        
        // 由于当前实现会识别并跳过已有traceId，所以我们直接断言不变
        String secondResult = parserWithAllOptions.parseAndModify(firstResult, anotherTraceId);
        logger.info("测试不同traceId值重复处理 - 第二次处理结果 (traceId={}): {}", anotherTraceId, secondResult);
        
        // 应该保持不变，因为识别到已经有traceId了
        assertEquals(firstResult, secondResult, "已有traceId的SQL不应该被修改");
        
        // 应该只包含原始的traceId值
        assertTrue(secondResult.contains("'" + testTraceId + "'"), "应该保留原始traceId值");
    }
    
    // 测试混合SQL语句和traceId的处理
    @Test
    public void testProcessingSQLWithTraceIdString() {
        // 包含traceId字符串但不是作为列的SQL
        String sql = "SELECT username, email FROM users WHERE comment LIKE '%" + testTraceId + "%'";
        
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        logger.info("测试SQL包含traceId字符串 - 原始SQL: {}", sql);
        logger.info("测试SQL包含traceId字符串 - 结果SQL: {}", result);
        
        // 验证traceId列被正确添加
        assertTrue(result.contains("as xTraceId"));
        
        // 验证WHERE子句中的traceId保持不变
        assertTrue(result.toLowerCase().contains("like '%test-trace-id-1234567%'"));
        
        // 确保只有一个traceId作为列
        int traceIdColumnCount = countOccurrences(result, "as xTraceId");
        assertEquals(1, traceIdColumnCount, "应该只有一个traceId列");
    }
    
    // 测试位置引用的ORDER BY子句
    @Test
    public void testOrderByWithPositionalReference() {
        String sql = "SELECT name, age FROM users ORDER BY 1";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试位置引用ORDER BY - 原始SQL: {}", sql);
        logger.info("测试位置引用ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by 2"));
        assertFalse(result.toLowerCase().contains("order by 1"));
    }

    // 测试子查询和位置引用的ORDER BY子句
    @Test
    public void testOrderByWithPositionalAndSubQueryReference() {
        String sql = "SELECT name, age FROM users WHERE id IN (SELECT id FROM t_users_his WHERE province ='sichuan') ORDER BY 1";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试子查询和位置引用ORDER BY - 原始SQL: {}", sql);
        logger.info("测试子查询和位置引用ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by 2"));
        assertFalse(result.toLowerCase().contains("order by 1"));
    }

    // 测试多位置引用的ORDER BY子句
    @Test
    public void testOrderByWithMultiplePositionalReferences() {
        String sql = "SELECT name, age, city FROM users ORDER BY 1, 3";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试多位置引用ORDER BY - 原始SQL: {}", sql);
        logger.info("测试多位置引用ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by 2, 4"));
        assertFalse(result.toLowerCase().contains("order by 1, 3"));
    }

    // 测试列名引用的ORDER BY子句
    @Test
    public void testOrderByWithColumnReferences() {
        String sql = "SELECT name, age, city FROM users ORDER BY age, city";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试列名引用ORDER BY - 原始SQL: {}", sql);
        logger.info("测试列名引用ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by age, city"));
    }

    // 测试混合引用的ORDER BY子句
    @Test
    public void testOrderByWithMixedReferences() {
        String sql = "SELECT name, age FROM users ORDER BY name DESC, 2 ASC";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试混合引用ORDER BY - 原始SQL: {}", sql);
        logger.info("测试混合引用ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by name desc, 3 asc"));
        assertFalse(result.toLowerCase().contains("order by name desc, 2 asc"));
    }

    // 测试ORDER BY中的表达式
    @Test
    public void testOrderByWithExpression() {
        String sql = "SELECT name, age FROM users ORDER BY age * 2 DESC";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试表达式ORDER BY - 原始SQL: {}", sql);
        logger.info("测试表达式ORDER BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by age * 2 desc"));
        // 确保没有错误地修改为age * 3
        assertFalse(result.contains("age * 3"));
    }

    // 测试复杂查询
    @Test
    public void testComplexQuery() {
        String sql = "SELECT u.name, COUNT(o.id) AS order_count " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id " +
                "WHERE u.status = 'active' " +
                "GROUP BY u.name " +
                "HAVING COUNT(o.id) > 5 " +
                "ORDER BY 2 DESC, u.name ASC";

        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试复杂查询 - 原始SQL: {}", sql);
        logger.info("测试复杂查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("order by 3 desc"));
        assertFalse(result.toLowerCase().contains("order by 2 desc"));
    }

    // 测试SELECT中的子查询
    @Test
    public void testSubqueryInSelectClause() {
        String sql = "SELECT name, (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) AS order_count FROM users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试SELECT中的子查询 - 原始SQL: {}", sql);
        logger.info("测试SELECT中的子查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("order_count"));
    }

    // 测试UNION查询
    @Test
    public void testUnionQuery() {
        String sql = "SELECT name, age FROM users WHERE status = 'active' UNION SELECT name, age FROM archived_users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试UNION查询 - 原始SQL: {}", sql);
        logger.info("测试UNION查询 - 结果SQL: {}", result);

        assertFalse(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("union"));
    }

    // 测试窗口函数
    @Test
    public void testWindowFunctions() {
        String sql = "SELECT name, salary, RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rank FROM employees";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试窗口函数 - 原始SQL: {}", sql);
        logger.info("测试窗口函数 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("rank() over"));
    }

    // 测试使用公共表表达式的查询
    @Test
    public void testCommonTableExpression() {
        String sql = "WITH ranked_employees AS (SELECT name, department, RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rank FROM employees) " +
                "SELECT name, department FROM ranked_employees WHERE rank <= 3";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试公共表表达式 - 原始SQL: {}", sql);
        logger.info("测试公共表表达式 - 结果SQL: {}", result);

        assertFalse(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("with ranked_employees"));
    }

    // 测试带ON子句的JOIN
    @Test
    public void testJoinWithOnClause() {
        String sql = "SELECT u.name, o.order_date FROM users u JOIN orders o ON u.id = o.user_id WHERE o.status = 'completed'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带ON子句的JOIN - 原始SQL: {}", sql);
        logger.info("测试带ON子句的JOIN - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("join orders"));
    }

    // 测试多个JOIN
    @Test
    public void testMultipleJoins() {
        String sql = "SELECT u.name, o.order_id, p.name AS product_name " +
                "FROM users u " +
                "JOIN orders o ON u.id = o.user_id " +
                "JOIN order_items i ON o.id = i.order_id " +
                "JOIN products p ON i.product_id = p.id";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试多个JOIN - 原始SQL: {}", sql);
        logger.info("测试多个JOIN - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("join orders"));
        assertTrue(result.toLowerCase().contains("join order_items"));
        assertTrue(result.toLowerCase().contains("join products"));
    }

    // 测试带CASE语句的SELECT
    @Test
    public void testSelectWithCaseStatement() {
        String sql = "SELECT name, CASE WHEN age < 18 THEN 'Minor' WHEN age >= 18 AND age < 65 THEN 'Adult' ELSE 'Senior' END AS age_group FROM users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试CASE语句 - 原始SQL: {}", sql);
        logger.info("测试CASE语句 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("case"));
        assertTrue(result.toLowerCase().contains("when"));
    }

    // 测试LIMIT和OFFSET
    @Test
    public void testLimitAndOffset() {
        String sql = "SELECT name, email FROM users ORDER BY name LIMIT 10 OFFSET 20";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试LIMIT和OFFSET - 原始SQL: {}", sql);
        logger.info("测试LIMIT和OFFSET - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("limit"));
        assertTrue(result.contains("20"));
    }

    // 测试带HAVING的GROUP BY
    @Test
    public void testGroupByWithHaving() {
        String sql = "SELECT department, AVG(salary) AS avg_salary FROM employees GROUP BY department HAVING AVG(salary) > 50000";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带HAVING的GROUP BY - 原始SQL: {}", sql);
        logger.info("测试带HAVING的GROUP BY - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("group by"));
        assertTrue(result.toLowerCase().contains("having avg"));
    }

    // 测试DISTINCT查询
    @Test
    public void testDistinctQuery() {
        String sql = "SELECT DISTINCT department FROM employees";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试DISTINCT查询 - 原始SQL: {}", sql);
        logger.info("测试DISTINCT查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("distinct"));
    }

    // 测试复杂WHERE子句
    @Test
    public void testComplexWhereClause() {
        String sql = "SELECT name, age FROM users WHERE (status = 'active' AND age > 21) OR (membership_level = 'premium' AND joined_date < '2022-01-01')";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试复杂WHERE子句 - 原始SQL: {}", sql);
        logger.info("测试复杂WHERE子句 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("where (status"));
    }

    // 测试嵌套子查询
    @Test
    public void testNestedSubqueries() {
        String sql = "SELECT name FROM users WHERE id IN (SELECT user_id FROM orders WHERE order_date > '2022-01-01' AND status IN (SELECT code FROM order_status WHERE description = 'Completed'))";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试嵌套子查询 - 原始SQL: {}", sql);
        logger.info("测试嵌套子查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("where id in"));
    }

    // 测试聚合函数
    @Test
    public void testAggregateFunctions() {
        String sql = "SELECT department, MIN(salary) AS min_salary, MAX(salary) AS max_salary, AVG(salary) AS avg_salary, SUM(salary) AS total_salary FROM employees GROUP BY department";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试聚合函数 - 原始SQL: {}", sql);
        logger.info("测试聚合函数 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("min(salary)"));
        assertTrue(result.toLowerCase().contains("max(salary)"));
    }

    // 测试带日期函数的SELECT
    @Test
    public void testSelectWithDateFunctions() {
        String sql = "SELECT name, YEAR(birth_date) AS birth_year, MONTH(birth_date) AS birth_month FROM users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试日期函数 - 原始SQL: {}", sql);
        logger.info("测试日期函数 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("year(birth_date)"));
        assertTrue(result.toLowerCase().contains("month(birth_date)"));
    }

    // 测试EXISTS子查询
    @Test
    public void testExistsSubquery() {
        String sql = "SELECT name FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.status = 'completed')";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试EXISTS子查询 - 原始SQL: {}", sql);
        logger.info("测试EXISTS子查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("exists"));
    }

    // 测试带子查询的INSERT语句
    @Test
    public void testInsertWithSubquery() {
        String sql = "INSERT INTO active_users SELECT id, name, email FROM users WHERE status = 'active'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带子查询的INSERT - 原始SQL: {}", sql);
        logger.info("测试带子查询的INSERT - 结果SQL: {}", result);

        // INSERT语句不应被修改
        assertEquals(sql, result);
    }

    // 测试带括号的复杂SELECT
    @Test
    public void testSelectWithParentheses() {
        String sql = "SELECT * FROM (SELECT name, age FROM users WHERE status = 'active') AS active_users";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带括号的复杂SELECT - 原始SQL: {}", sql);
        logger.info("测试带括号的复杂SELECT - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
    }

    // 测试使用别名的查询
    @Test
    public void testSelectWithAliases() {
        String sql = "SELECT u.name AS user_name, u.email AS user_email FROM users u WHERE u.status = 'active'";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试使用别名的查询 - 原始SQL: {}", sql);
        logger.info("测试使用别名的查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("user_name"));
        assertTrue(result.toLowerCase().contains("user_email"));
    }

    // 测试带函数的WHERE子句
    @Test
    public void testWhereWithFunctions() {
        String sql = "SELECT name, created_at FROM users WHERE DATEDIFF(NOW(), created_at) <= 30";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带函数的WHERE子句 - 原始SQL: {}", sql);
        logger.info("测试带函数的WHERE子句 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("datediff"));
    }

    // 测试带IN子句的查询
    @Test
    public void testSelectWithInClause() {
        String sql = "SELECT name, department FROM employees WHERE department IN ('HR', 'IT', 'Finance')";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带IN子句的查询 - 原始SQL: {}", sql);
        logger.info("测试带IN子句的查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("in ('hr'"));
    }

    // 测试带BETWEEN子句的查询
    @Test
    public void testSelectWithBetweenClause() {
        String sql = "SELECT name, age FROM users WHERE age BETWEEN 18 AND 65";
        String result = parserWithAllOptions.parseAndModify(sql, testTraceId);
        
        logger.info("测试带BETWEEN子句的查询 - 原始SQL: {}", sql);
        logger.info("测试带BETWEEN子句的查询 - 结果SQL: {}", result);

        assertTrue(result.contains("'" + testTraceId + "'"));
        assertTrue(result.toLowerCase().contains("between 18 and 65"));
    }
    
    // 辅助方法：计算字符串在文本中出现的次数
    private int countOccurrences(String text, String searchString) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(searchString, index)) != -1) {
            count++;
            index += searchString.length();
        }
        return count;
    }
}