package com.patrick.sqltrace;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.patrick.sqltrace.SqlTraceSDK;
import com.patrick.sqltrace.core.SqlTraceContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlTraceSDK的单元测试类
 */
public class SqlTraceSDKTest {

    @AfterEach
    public void cleanup() {
        // 每个测试后清理线程本地上下文
        SqlTraceContext.clear();
    }

    @Test
    public void testInitTraceWithGeneration() {
        // 测试自动生成的TraceId
        String traceId = SqlTraceSDK.initTrace();

        // 验证生成的TraceId不为空且长度为32（UUID去掉连字符）
        assertNotNull(traceId);
        assertEquals(32, traceId.length());

        // 验证可以从上下文中获取相同的TraceId
        assertEquals(traceId, SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    public void testInitTraceWithProvidedId() {
        // 测试使用提供的TraceId
        String customTraceId = "custom-trace-123456";
        SqlTraceSDK.initTrace(customTraceId);

        // 验证上下文中设置了正确的TraceId
        assertEquals(customTraceId, SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    public void testInitTraceWithEmptyId() {
        // 测试使用空TraceId应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            SqlTraceSDK.initTrace("");
        });
    }

    @Test
    public void testInitTraceWithNullId() {
        // 测试使用null TraceId应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            SqlTraceSDK.initTrace(null);
        });
    }

    @Test
    public void testClearTrace() {
        // 先设置一个TraceId
        SqlTraceSDK.initTrace("test-trace-id");

        // 验证已设置
        assertEquals("test-trace-id", SqlTraceSDK.getCurrentTraceId());

        // 清除TraceId
        SqlTraceSDK.clearTrace();

        // 验证已清除
        assertEquals("", SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    public void testGetCurrentTraceIdWhenNotSet() {
        // 确保上下文已清除
        SqlTraceContext.clear();

        // 验证未设置时返回空字符串
        assertEquals("", SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    public void testTraceIdUniqueness() {
        // 测试多次生成的TraceId是唯一的
        String traceId1 = SqlTraceSDK.initTrace();
        SqlTraceContext.clear(); // 清除以便生成新的
        String traceId2 = SqlTraceSDK.initTrace();

        // 验证两个TraceId不同
        assertNotEquals(traceId1, traceId2);
    }
}