package com.patrick.sqltrace.core;

/**
 * 线程本地上下文，管理当前线程的追踪ID
 */
public class SqlTraceContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        return traceId != null ? traceId : "";
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
