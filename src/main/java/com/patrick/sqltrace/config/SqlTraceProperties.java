package com.patrick.sqltrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQL追踪配置属性
 */
@ConfigurationProperties(prefix = "sql.xtrace")
public class SqlTraceProperties {

    /**
     * 是否启用SQL追踪
     */
    private boolean enabled = true;

    /**
     * 注入到SELECT语句中的traceId列别名
     */
    private String traceIdFieldName = "xTraceId";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTraceIdFieldName() {
        return traceIdFieldName;
    }

    public void setTraceIdFieldName(String traceIdFieldName) {
        this.traceIdFieldName = traceIdFieldName;
    }
}
