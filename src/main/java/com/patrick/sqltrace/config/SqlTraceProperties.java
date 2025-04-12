package com.patrick.sqltrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQL追踪功能的配置属性类
 *
 * @author patrickz
 * @since 2025-03-12
 */
@ConfigurationProperties(prefix = "sql.xtrace")
public class SqlTraceProperties {

    /**
     * 启用SQL追踪功能
     */
    private boolean enabled = true;

    /**
     * 启用对COUNT(*)查询的追踪
     */
    private boolean enableCountQueries = false;

    /**
     * 启用对GROUP BY查询的追踪
     */
    private boolean enableGroupByQueries = false;

    /**
     * 添加到SELECT语句中的追踪ID字段名
     */
    private String traceIdFieldName = "xTraceId";

    /**
     * 最大页面大小，超过此值的LIMIT将被重写
     */
    private int maxPageSize = 20000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableCountQueries() {
        return enableCountQueries;
    }

    public void setEnableCountQueries(boolean enableCountQueries) {
        this.enableCountQueries = enableCountQueries;
    }

    public boolean isEnableGroupByQueries() {
        return enableGroupByQueries;
    }

    public void setEnableGroupByQueries(boolean enableGroupByQueries) {
        this.enableGroupByQueries = enableGroupByQueries;
    }

    public String getTraceIdFieldName() {
        return traceIdFieldName;
    }

    public void setTraceIdFieldName(String traceIdFieldName) {
        this.traceIdFieldName = traceIdFieldName;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}