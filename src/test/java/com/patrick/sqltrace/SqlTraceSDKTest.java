package com.patrick.sqltrace;

import com.patrick.sqltrace.core.SqlTraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlTraceSDKTest {

    @AfterEach
    void cleanup() {
        SqlTraceContext.clear();
    }

    @Test
    void initTrace_generatesUniqueId() {
        String id1 = SqlTraceSDK.initTrace();
        SqlTraceContext.clear();
        String id2 = SqlTraceSDK.initTrace();
        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(32, id1.length());
        assertNotEquals(id1, id2);
    }

    @Test
    void initTrace_setsToContext() {
        String id = SqlTraceSDK.initTrace();
        assertEquals(id, SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    void initTrace_withCustomId() {
        SqlTraceSDK.initTrace("custom-trace-123");
        assertEquals("custom-trace-123", SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    void initTrace_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlTraceSDK.initTrace(null));
    }

    @Test
    void initTrace_emptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> SqlTraceSDK.initTrace(""));
    }

    @Test
    void clearTrace_removesId() {
        SqlTraceSDK.initTrace();
        SqlTraceSDK.clearTrace();
        assertEquals("", SqlTraceSDK.getCurrentTraceId());
    }

    @Test
    void getCurrentTraceId_emptyByDefault() {
        assertEquals("", SqlTraceSDK.getCurrentTraceId());
    }
}
