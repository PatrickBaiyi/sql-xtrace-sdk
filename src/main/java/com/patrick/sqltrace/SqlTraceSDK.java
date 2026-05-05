package com.patrick.sqltrace;

import com.patrick.sqltrace.core.SqlTraceContext;

import java.util.UUID;

/**
 * 公共API，提供手动设置/清除追踪ID的能力
 */
public class SqlTraceSDK {

    public static String initTrace() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        SqlTraceContext.setTraceId(traceId);
        return traceId;
    }

    public static void initTrace(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            throw new IllegalArgumentException("traceId不能为null或空");
        }
        SqlTraceContext.setTraceId(traceId);
    }

    public static String getCurrentTraceId() {
        return SqlTraceContext.getTraceId();
    }

    public static void clearTrace() {
        SqlTraceContext.clear();
    }
}
