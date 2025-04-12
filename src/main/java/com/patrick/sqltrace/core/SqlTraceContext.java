package com.patrick.sqltrace.core;

/**
 * 线程本地上下文类，用于管理当前线程的追踪ID。
 *
 * @author patrickz
 * @since 2025-03-12
 */
public class SqlTraceContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 为当前线程设置追踪ID
     *
     * @param traceId 要设置的追踪ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 获取当前线程的追踪ID
     *
     * @return 当前的追踪ID，如果没有设置则返回空字符串
     */
    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        return traceId != null ? traceId : "";
    }

    /**
     * 清除当前线程的追踪ID
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}