package com.patrick.sqltrace.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SQL解析器：向所有SELECT语句注入traceId列
 */
public class TraceIdSqlParser {
    private static final Logger logger = LoggerFactory.getLogger(TraceIdSqlParser.class);

    private final String traceIdFieldName;

    public TraceIdSqlParser(String traceIdFieldName) {
        this.traceIdFieldName = traceIdFieldName;
    }

    /**
     * 解析并修改SQL，向SELECT语句注入traceId列
     *
     * @param originalSql 原始SQL
     * @param traceId     追踪ID值
     * @return 修改后的SQL，非SELECT或解析失败则返回原始SQL
     */
    public String parseAndModify(String originalSql, String traceId) {
        try {
            if (!isSelectStatement(originalSql)) {
                return originalSql;
            }

            if (containsTraceIdField(originalSql)) {
                return originalSql;
            }

            List<SQLStatement> stmtList = SQLUtils.parseStatements(originalSql, JdbcConstants.MYSQL);
            if (stmtList.isEmpty()) {
                return originalSql;
            }

            SQLStatement stmt = stmtList.get(0);
            if (!(stmt instanceof SQLSelectStatement)) {
                return originalSql;
            }

            SQLSelectStatement selectStmt = (SQLSelectStatement) stmt;
            SQLSelectQuery query = selectStmt.getSelect().getQuery();
            injectTraceId(query, traceId);

            return SQLUtils.toSQLString(stmt, JdbcConstants.MYSQL,
                    new SQLUtils.FormatOption(false, false));
        } catch (Exception e) {
            logger.warn("SQL解析失败，返回原始SQL: {}", originalSql, e);
            return originalSql;
        }
    }

    private void injectTraceId(SQLSelectQuery query, String traceId) {
        if (query instanceof MySqlSelectQueryBlock) {
            MySqlSelectQueryBlock block = (MySqlSelectQueryBlock) query;
            SQLSelectItem item = new SQLSelectItem(new SQLCharExpr(traceId), traceIdFieldName);
            block.getSelectList().add(item);
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery union = (SQLUnionQuery) query;
            injectTraceId(union.getLeft(), traceId);
            injectTraceId(union.getRight(), traceId);
        }
    }

    private boolean isSelectStatement(String sql) {
        // 快速前缀检测，避免不必要的解析
        int i = 0;
        int len = sql.length();
        while (i < len && sql.charAt(i) <= ' ') {
            i++;
        }
        if (i >= len) {
            return false;
        }
        char c0 = Character.toUpperCase(sql.charAt(i));
        // SELECT 或 WITH (CTE) 开头的都是查询语句
        return c0 == 'S' || c0 == 'W' || c0 == '(';
    }

    private boolean containsTraceIdField(String sql) {
        // 检测 AS xTraceId 避免重复注入
        String lower = sql.toLowerCase();
        String fieldLower = traceIdFieldName.toLowerCase();
        return lower.contains(" as " + fieldLower)
                || lower.contains(" as `" + fieldLower + "`");
    }
}
