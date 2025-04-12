package com.patrick.sqltrace.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 用于向SELECT语句添加追踪ID的SQL解析器，并限制大查询
 */
public class TraceIdSqlParser {
    private static final Logger logger = LoggerFactory.getLogger(TraceIdSqlParser.class);

    private final boolean enableCountQueries;
    private final boolean enableGroupByQueries;
    private final String traceIdFieldName;
    private final int maxPageSize;

    public TraceIdSqlParser(boolean enableCountQueries, boolean enableGroupByQueries,
                            String traceIdFieldName, int maxPageSize) {
        this.enableCountQueries = enableCountQueries;
        this.enableGroupByQueries = enableGroupByQueries;
        this.traceIdFieldName = traceIdFieldName;
        this.maxPageSize = maxPageSize;
    }

    /**
     * 解析并修改SQL以添加追踪ID到SELECT语句，并检查/修改LIMIT子句
     *
     * @param originalSql 原始SQL语句
     * @param traceId 要添加的追踪ID
     * @return 添加了追踪ID的修改后SQL
     */
    public String parseAndModify(String originalSql, String traceId) {
        try {
            // 检查是否为SELECT语句
            if (!isSelectStatement(originalSql)) {
                return originalSql;
            }
            
            // 检查SQL是否已包含traceId字段，避免重复添加
            if (containsTraceIdField(originalSql)) {
                logger.debug("SQL已包含traceId字段，跳过添加: {}", originalSql);
                return originalSql;
            }

            // 检查是否是COUNT查询
            boolean isCountQuery = originalSql.toUpperCase().contains("COUNT(");
            // 检查是否是GROUP BY查询
            boolean isGroupByQuery = originalSql.toUpperCase().contains("GROUP BY");

            // 如果是COUNT查询且enableCountQueries为false，则跳过修改
            if (isCountQuery && !enableCountQueries) {
                return originalSql;
            }

            // 如果是GROUP BY查询且enableGroupByQueries为false，则跳过修改
            if (isGroupByQuery && !enableGroupByQueries) {
                return originalSql;
            }

            // 解析SQL语句
            List<SQLStatement> stmtList = SQLUtils.parseStatements(originalSql, JdbcConstants.MYSQL);

            if (stmtList.isEmpty()) {
                return originalSql;
            }

            SQLStatement stmt = stmtList.get(0);

            if (!(stmt instanceof SQLSelectStatement)) {
                return originalSql;
            }

            SQLSelectStatement selectStmt = (SQLSelectStatement) stmt;
            SQLSelect select = selectStmt.getSelect();
            SQLSelectQuery query = select.getQuery();

            // 处理UNION查询
            if (query instanceof SQLUnionQuery) {
                SQLUnionQuery unionQuery = (SQLUnionQuery) query;
                // 修改左侧查询
                modifyUnionPart(unionQuery.getLeft(), traceId);
                // 修改右侧查询
                modifyUnionPart(unionQuery.getRight(), traceId);
            } else if (query instanceof MySqlSelectQueryBlock) {
                modifySelectQueryBlock((MySqlSelectQueryBlock) query, traceId);
            }

            // 使用不格式化的选项生成SQL，保持原始格式
            String modifiedSql = SQLUtils.toSQLString(stmt, JdbcConstants.MYSQL,
                    new SQLUtils.FormatOption(false, false));
            logger.debug("处理后SQL: {}", modifiedSql);
            return modifiedSql;
        } catch (Exception e) {
            logger.warn("解析和修改SQL失败: {}", originalSql, e);
            return originalSql;
        }
    }

    /**
     * 修改UNION查询的一部分
     */
    private void modifyUnionPart(SQLSelectQuery query, String traceId) {
        if (query instanceof MySqlSelectQueryBlock) {
            modifySelectQueryBlock((MySqlSelectQueryBlock) query, traceId);
        } else if (query instanceof SQLSelect) {
            SQLSelect select = (SQLSelect) query;
            if (select.getQuery() instanceof MySqlSelectQueryBlock) {
                modifySelectQueryBlock((MySqlSelectQueryBlock) select.getQuery(), traceId);
            }
        } else if (query instanceof SQLUnionQuery) {
            // 处理嵌套的UNION
            SQLUnionQuery nestedUnion = (SQLUnionQuery) query;
            modifyUnionPart(nestedUnion.getLeft(), traceId);
            modifyUnionPart(nestedUnion.getRight(), traceId);
        }
    }

    /**
     * 修改SELECT查询块
     */
    private void modifySelectQueryBlock(MySqlSelectQueryBlock queryBlock, String traceId) {
        // 检查是否是简单的聚合查询或全聚合查询
        if (isSimpleAggregateQuery(queryBlock)) {
            // 对于简单的聚合查询，不添加traceId列
            logger.debug("跳过向简单聚合查询添加traceId: {}", queryBlock);
            return;
        }

        // 添加追踪ID列
        SQLSelectItem traceIdItem = new SQLSelectItem();
        SQLCharExpr traceExpr = new SQLCharExpr(traceId);
        traceIdItem.setExpr(traceExpr);
        traceIdItem.setAlias(traceIdFieldName);

        // 总是将traceId添加到SELECT列表的末尾
        queryBlock.getSelectList().add(traceIdItem);

        // 处理ORDER BY子句
        SQLOrderBy orderBy = queryBlock.getOrderBy();
        if (orderBy != null) {
            for (SQLSelectOrderByItem orderItem : orderBy.getItems()) {
                SQLExpr expr = orderItem.getExpr();

                // 只处理纯整数表达式，即位置引用
                if (expr instanceof SQLIntegerExpr) {
                    SQLIntegerExpr intExpr = (SQLIntegerExpr) expr;
                    int position = intExpr.getNumber().intValue();

                    // 不需要调整位置值，因为traceId总是添加在末尾
                }
            }
        }

        // 处理LIMIT子句
        SQLLimit limit = queryBlock.getLimit();
        if (limit == null) {
            // 没有LIMIT子句，添加一个
            limit = new SQLLimit(new SQLIntegerExpr(maxPageSize));
            queryBlock.setLimit(limit);
            logger.warn("查询没有LIMIT子句，已自动添加LIMIT {}", maxPageSize);
        } else {
            // 有LIMIT子句，检查rowCount
            SQLExpr rowCountExpr = limit.getRowCount();
            if (rowCountExpr instanceof SQLIntegerExpr) {
                int rowCount = ((SQLIntegerExpr) rowCountExpr).getNumber().intValue();
                if (rowCount > maxPageSize) {
                    limit.setRowCount(new SQLIntegerExpr(maxPageSize));
                    logger.warn("LIMIT子句的pageSize({})超过了最大值，已重写为{}", rowCount, maxPageSize);
                }
            } else if (rowCountExpr instanceof SQLNumberExpr) {
                double rowCount = ((SQLNumberExpr) rowCountExpr).getNumber().doubleValue();
                if (rowCount > maxPageSize) {
                    limit.setRowCount(new SQLIntegerExpr(maxPageSize));
                    logger.warn("LIMIT子句的pageSize({})超过了最大值，已重写为{}", rowCount, maxPageSize);
                }
            }
        }
    }

    /**
     * 检查是否是简单的聚合查询或全聚合查询
     * 简单聚合查询指的是：
     * 1. 只有一个聚合函数，没有其他列的查询
     *    例如：SELECT COUNT(*) FROM users
     *         SELECT MAX(id) FROM users
     *         SELECT MIN(salary) FROM users
     * 2. 所有列都是聚合函数的查询
     *    例如：SELECT COUNT(*), MAX(salary), MIN(salary), AVG(salary) FROM users
     * 而不是：SELECT COUNT(*), name FROM users GROUP BY name
     */
    private boolean isSimpleAggregateQuery(MySqlSelectQueryBlock queryBlock) {
        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        
        // 如果没有列，不是聚合查询
        if (selectList.isEmpty()) {
            return false;
        }

        // 检查每一列是否都是聚合函数
        for (SQLSelectItem item : selectList) {
            SQLExpr expr = item.getExpr();
            String exprStr = expr.toString().toUpperCase();
            
            // 如果有任何一列不是聚合函数，就不是简单聚合查询
            boolean isAggregateFunction = 
                exprStr.startsWith("COUNT(") ||
                exprStr.startsWith("MAX(") ||
                exprStr.startsWith("MIN(") ||
                exprStr.startsWith("AVG(") ||
                exprStr.startsWith("SUM(");
            
            if (!isAggregateFunction) {
                return false;
            }
        }
        
        // 如果所有列都是聚合函数，则是简单聚合查询
        return true;
    }

    /**
     * 检查SQL是否为SELECT语句
     *
     * @param sql 要检查的SQL
     * @return 如果是SELECT语句则返回true
     */
    private boolean isSelectStatement(String sql) {
        String trimmedSql = sql.trim().toUpperCase();
        return trimmedSql.startsWith("SELECT ");
    }
    
    /**
     * 检查SQL是否已包含traceId字段，避免重复添加
     * 
     * @param sql 要检查的SQL
     * @return 如果SQL已包含traceId字段则返回true
     */
    private boolean containsTraceIdField(String sql) {
        // 检查是否包含"as traceIdFieldName"或"AS traceIdFieldName"
        String lowerCaseSql = sql.toLowerCase();
        String lowerCaseFieldName = traceIdFieldName.toLowerCase();
        
        if (lowerCaseSql.contains(" as " + lowerCaseFieldName) || 
            lowerCaseSql.contains(" AS " + lowerCaseFieldName)) {
            return true;
        }
        
        // 检查是否包含字段名本身作为列名
        if (lowerCaseSql.contains(", " + lowerCaseFieldName + ",") || 
            lowerCaseSql.contains(", " + lowerCaseFieldName + " ") || 
            lowerCaseSql.contains("select " + lowerCaseFieldName + ",") || 
            lowerCaseSql.contains("select " + lowerCaseFieldName + " ")) {
            return true;
        }
        
        return false;
    }
}