package com.patrick.sqltrace.core;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqlTraceInterceptorTest {

    private SqlTraceInterceptor interceptor;
    private Executor executor;
    private Configuration configuration;

    @BeforeEach
    void setup() {
        interceptor = new SqlTraceInterceptor("xTraceId");
        executor = mock(Executor.class);
        configuration = new Configuration();
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
        SqlTraceContext.clear();
    }

    @Test
    void noTraceId_proceedsWithoutModification() throws Throwable {
        MappedStatement ms = createMappedStatement("SELECT id FROM users");
        Object[] args = {ms, null, RowBounds.DEFAULT, mock(ResultHandler.class)};
        Invocation invocation = new Invocation(executor, Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class), args);

        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class), any(ResultHandler.class)))
                .thenReturn(Collections.emptyList());
        interceptor.intercept(invocation);
        verify(executor).query(eq(ms), any(), any(RowBounds.class), any(ResultHandler.class));
    }

    @Test
    void withMdcTraceId_modifiesSql() throws Throwable {
        MDC.put("traceId", "test-trace-001");
        MappedStatement ms = createMappedStatement("SELECT id FROM users");
        BoundSql boundSql = ms.getBoundSql(null);

        Object[] args = {ms, null, RowBounds.DEFAULT, mock(ResultHandler.class),
                new CacheKey(), boundSql};

        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class),
                any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class)))
                .thenReturn(Collections.emptyList());

        Invocation invocation = new Invocation(executor, Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class), args);

        interceptor.intercept(invocation);

        verify(executor).query(argThat(newMs -> {
            String sql = newMs.getBoundSql(null).getSql().toLowerCase();
            return sql.contains("test-trace-001") && sql.contains("xtraceid");
        }), any(), any(RowBounds.class), any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class));
    }

    @Test
    void withContextTraceId_modifiesSql() throws Throwable {
        SqlTraceContext.setTraceId("ctx-trace-002");
        MappedStatement ms = createMappedStatement("SELECT name FROM products");
        BoundSql boundSql = ms.getBoundSql(null);

        Object[] args = {ms, null, RowBounds.DEFAULT, mock(ResultHandler.class),
                new CacheKey(), boundSql};

        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class),
                any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class)))
                .thenReturn(Collections.emptyList());

        Invocation invocation = new Invocation(executor, Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class), args);

        interceptor.intercept(invocation);

        verify(executor).query(argThat(newMs -> {
            String sql = newMs.getBoundSql(null).getSql();
            return sql.contains("ctx-trace-002");
        }), any(), any(RowBounds.class), any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class));
    }

    @Test
    void mdcPriority_mideaApmFirst() throws Throwable {
        MDC.put("midea-apm-traceid", "apm-trace");
        MDC.put("tid", "tid-trace");
        MDC.put("traceId", "generic-trace");
        SqlTraceContext.setTraceId("ctx-trace");

        MappedStatement ms = createMappedStatement("SELECT id FROM users");
        BoundSql boundSql = ms.getBoundSql(null);

        Object[] args = {ms, null, RowBounds.DEFAULT, mock(ResultHandler.class),
                new CacheKey(), boundSql};

        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class),
                any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class)))
                .thenReturn(Collections.emptyList());

        Invocation invocation = new Invocation(executor, Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class), args);

        interceptor.intercept(invocation);

        verify(executor).query(argThat(newMs -> {
            String sql = newMs.getBoundSql(null).getSql();
            return sql.contains("apm-trace");
        }), any(), any(RowBounds.class), any(ResultHandler.class), any(CacheKey.class), any(BoundSql.class));
    }

    @Test
    void nonSelectStatement_noModification() throws Throwable {
        MDC.put("traceId", "test-trace");
        MappedStatement ms = createMappedStatement("UPDATE users SET name = 'x' WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        Object[] args = {ms, null, RowBounds.DEFAULT, mock(ResultHandler.class),
                new CacheKey(), boundSql};

        when(executor.query(eq(ms), any(), any(RowBounds.class),
                any(ResultHandler.class), any(CacheKey.class), eq(boundSql)))
                .thenReturn(Collections.emptyList());

        Invocation invocation = new Invocation(executor, Executor.class.getMethod("query",
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class), args);

        interceptor.intercept(invocation);

        // Should use original ms since SQL wasn't modified
        verify(executor).query(eq(ms), any(), any(RowBounds.class),
                any(ResultHandler.class), any(CacheKey.class), eq(boundSql));
    }

    private MappedStatement createMappedStatement(String sql) {
        SqlSource sqlSource = parameterObject -> new BoundSql(configuration, sql,
                new ArrayList<>(), parameterObject);

        ResultMap resultMap = new ResultMap.Builder(configuration, "testResultMap",
                Object.class, new ArrayList<>()).build();

        return new MappedStatement.Builder(configuration, "testStatement", sqlSource, SqlCommandType.SELECT)
                .resultMaps(Collections.singletonList(resultMap))
                .build();
    }
}
