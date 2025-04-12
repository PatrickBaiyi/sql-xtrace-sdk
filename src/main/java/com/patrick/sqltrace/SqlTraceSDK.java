package com.patrick.sqltrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.patrick.sqltrace.core.SqlTraceContext;

import java.util.UUID;

/**
 * 主SDK类，提供初始化和清除追踪ID的公共API。
 * 暂时不用
 *
 * @author patrickz
 * @since 2025-03-12
 */
public class SqlTraceSDK {
    private static final Logger logger = LoggerFactory.getLogger(SqlTraceSDK.class);

    /**
     * 使用生成的追踪ID为当前线程初始化追踪
     *
     * @return 生成的追踪ID
     */
    public static String initTrace() {
        String traceId = generateTraceId();
        SqlTraceContext.setTraceId(traceId);
        logger.debug("已使用ID初始化SQL追踪: {}", traceId);
        return traceId;
    }

    /**
     * 使用指定的追踪ID为当前线程初始化追踪
     *
     * @param traceId 要使用的追踪ID
     */
    public static void initTrace(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            throw new IllegalArgumentException("追踪ID不能为null或空");
        }
        SqlTraceContext.setTraceId(traceId);
        logger.debug("已使用提供的ID初始化SQL追踪: {}", traceId);
    }

    /**
     * 获取当前的追踪ID
     *
     * @return 当前的追踪ID，如果没有设置则返回空字符串
     */
    public static String getCurrentTraceId() {
        return SqlTraceContext.getTraceId();
    }

    /**
     * 清除当前线程的追踪ID
     */
    public static void clearTrace() {
        String traceId = SqlTraceContext.getTraceId();
        SqlTraceContext.clear();
        logger.debug("已清除SQL追踪ID: {}", traceId);
    }

    /**
     * 生成新的唯一追踪ID
     *
     * @return 新的追踪ID
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}