package com.patrick.sqltrace.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdSqlParserTest {

    private TraceIdSqlParser parser;
    private static final String TRACE_ID = "abc123def456";
    private static final String FIELD_NAME = "xTraceId";

    @BeforeEach
    void setup() {
        parser = new TraceIdSqlParser(FIELD_NAME);
    }

    // ========== 基础SELECT ==========

    @Nested
    @DisplayName("基础SELECT语句")
    class BasicSelect {

        @Test
        void simpleSelect() {
            String sql = "SELECT id, name FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectStar() {
            String sql = "SELECT * FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectWithWhere() {
            String sql = "SELECT id, name FROM users WHERE status = 'active'";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectWithAlias() {
            String sql = "SELECT u.id, u.name AS username FROM users u";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectDistinct() {
            String sql = "SELECT DISTINCT department FROM employees";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectWithLimit() {
            String sql = "SELECT id FROM users LIMIT 10";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
            assertTrue(result.contains("10"));
        }

        @Test
        void selectWithLimitOffset() {
            String sql = "SELECT id FROM users LIMIT 10 OFFSET 20";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== 聚合查询 ==========

    @Nested
    @DisplayName("聚合查询 - 全部注入")
    class AggregateQueries {

        @Test
        void countStar() {
            String sql = "SELECT COUNT(*) FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void countWithAlias() {
            String sql = "SELECT COUNT(*) AS total FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void multipleAggregates() {
            String sql = "SELECT COUNT(*), MAX(salary), MIN(salary), AVG(age) FROM employees";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void sumQuery() {
            String sql = "SELECT SUM(amount) FROM orders";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void countWithCondition() {
            String sql = "SELECT COUNT(*) FROM users WHERE age > 18";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== GROUP BY ==========

    @Nested
    @DisplayName("GROUP BY查询 - 全部注入")
    class GroupByQueries {

        @Test
        void simpleGroupBy() {
            String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void groupByWithHaving() {
            String sql = "SELECT department, AVG(salary) FROM employees GROUP BY department HAVING AVG(salary) > 50000";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void groupByMultipleColumns() {
            String sql = "SELECT department, city, COUNT(*) FROM employees GROUP BY department, city";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== JOIN ==========

    @Nested
    @DisplayName("JOIN查询")
    class JoinQueries {

        @Test
        void innerJoin() {
            String sql = "SELECT u.id, o.order_id FROM users u INNER JOIN orders o ON u.id = o.user_id";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void leftJoin() {
            String sql = "SELECT u.id, o.order_id FROM users u LEFT JOIN orders o ON u.id = o.user_id";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void multipleJoins() {
            String sql = "SELECT u.name, o.id, p.name FROM users u " +
                    "JOIN orders o ON u.id = o.user_id " +
                    "JOIN products p ON o.product_id = p.id";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== 子查询 ==========

    @Nested
    @DisplayName("子查询")
    class SubQueries {

        @Test
        void subqueryInWhere() {
            String sql = "SELECT id, name FROM users WHERE id IN (SELECT user_id FROM orders)";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void subqueryInFrom() {
            String sql = "SELECT t.id, t.cnt FROM (SELECT id, COUNT(*) as cnt FROM orders GROUP BY id) t";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void subqueryInSelect() {
            String sql = "SELECT id, (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) AS order_count FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== UNION ==========

    @Nested
    @DisplayName("UNION查询")
    class UnionQueries {

        @Test
        void simpleUnion() {
            String sql = "SELECT id, name FROM users UNION SELECT id, name FROM admins";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void unionAll() {
            String sql = "SELECT id, name FROM users UNION ALL SELECT id, name FROM admins";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== ORDER BY ==========

    @Nested
    @DisplayName("ORDER BY")
    class OrderByQueries {

        @Test
        void orderByColumn() {
            String sql = "SELECT id, name FROM users ORDER BY name ASC";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
            assertTrue(result.toUpperCase().contains("ORDER BY"));
        }

        @Test
        void orderByMultiple() {
            String sql = "SELECT id, name, age FROM users ORDER BY age DESC, name ASC";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== 窗口函数 ==========

    @Nested
    @DisplayName("窗口函数")
    class WindowFunctions {

        @Test
        void rowNumber() {
            String sql = "SELECT id, name, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void rankPartition() {
            String sql = "SELECT id, department, salary, RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rnk FROM employees";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== CTE ==========

    @Nested
    @DisplayName("CTE (WITH)")
    class CTEQueries {

        @Test
        void simpleCTE() {
            String sql = "WITH active_users AS (SELECT id, name FROM users WHERE status = 'active') SELECT id, name FROM active_users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== 非SELECT语句 - 不处理 ==========

    @Nested
    @DisplayName("非SELECT语句不处理")
    class NonSelectStatements {

        @Test
        void insertStatement() {
            String sql = "INSERT INTO users (name, email) VALUES ('test', 'test@test.com')";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertEquals(sql, result);
        }

        @Test
        void updateStatement() {
            String sql = "UPDATE users SET name = 'test' WHERE id = 1";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertEquals(sql, result);
        }

        @Test
        void deleteStatement() {
            String sql = "DELETE FROM users WHERE id = 1";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertEquals(sql, result);
        }
    }

    // ========== 防重复注入 ==========

    @Nested
    @DisplayName("防重复注入")
    class DuplicatePrevention {

        @Test
        void alreadyContainsTraceId() {
            String sql = "SELECT id, name, 'old-trace' AS xTraceId FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertEquals(sql, result);
        }

        @Test
        void doubleProcessing() {
            String sql = "SELECT id, name FROM users";
            String first = parser.parseAndModify(sql, TRACE_ID);
            String second = parser.parseAndModify(first, "another-trace");
            assertEquals(first, second);
        }
    }

    // ========== 自定义字段名 ==========

    @Test
    @DisplayName("自定义traceId字段名")
    void customFieldName() {
        TraceIdSqlParser customParser = new TraceIdSqlParser("myTrace");
        String sql = "SELECT id FROM users";
        String result = customParser.parseAndModify(sql, TRACE_ID);
        assertTrue(result.toLowerCase().contains("mytrace"));
    }

    // ========== 复杂实际场景 ==========

    @Nested
    @DisplayName("复杂实际场景")
    class ComplexRealWorld {

        @Test
        void caseWhen() {
            String sql = "SELECT id, CASE WHEN age > 18 THEN 'adult' ELSE 'minor' END AS category FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void dateFunctions() {
            String sql = "SELECT id, DATE_FORMAT(created_at, '%Y-%m-%d') AS dt FROM orders";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void existsSubquery() {
            String sql = "SELECT id, name FROM users WHERE EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void complexJoinWithAggregation() {
            String sql = "SELECT u.department, COUNT(o.id) AS order_count, SUM(o.amount) AS total " +
                    "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
                    "GROUP BY u.department HAVING COUNT(o.id) > 5 ORDER BY total DESC LIMIT 20";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void selectWithForUpdate() {
            String sql = "SELECT id, name FROM users WHERE id = 1 FOR UPDATE";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void mybatisPlaceholder() {
            String sql = "SELECT id, name FROM users WHERE id = ? AND status = ?";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void longComplexQuery() {
            String sql = "SELECT a.id, a.name, b.order_id, b.amount, " +
                    "c.product_name, d.category_name " +
                    "FROM users a " +
                    "INNER JOIN orders b ON a.id = b.user_id " +
                    "INNER JOIN products c ON b.product_id = c.id " +
                    "INNER JOIN categories d ON c.category_id = d.id " +
                    "WHERE a.status = 'active' AND b.created_at > '2024-01-01' " +
                    "ORDER BY b.created_at DESC LIMIT 100";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    // ========== 边界情况 ==========

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        void emptyString() {
            String result = parser.parseAndModify("", TRACE_ID);
            assertEquals("", result);
        }

        @Test
        void whitespaceOnly() {
            String result = parser.parseAndModify("   ", TRACE_ID);
            assertEquals("   ", result);
        }

        @Test
        void leadingWhitespace() {
            String sql = "   SELECT id FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void lowercaseSelect() {
            String sql = "select id, name from users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }

        @Test
        void mixedCaseSelect() {
            String sql = "SeLeCt id, name FROM users";
            String result = parser.parseAndModify(sql, TRACE_ID);
            assertContainsTraceId(result);
        }
    }

    private void assertContainsTraceId(String sql) {
        String lower = sql.toLowerCase();
        assertTrue(lower.contains(TRACE_ID.toLowerCase()),
                "SQL should contain traceId value: " + sql);
        assertTrue(lower.contains(FIELD_NAME.toLowerCase()),
                "SQL should contain field name: " + sql);
    }
}
