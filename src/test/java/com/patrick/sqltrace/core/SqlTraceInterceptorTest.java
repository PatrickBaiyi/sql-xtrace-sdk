package com.patrick.sqltrace.core;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SqlTraceInterceptor的单元测试类
 */
public class SqlTraceInterceptorTest {

    private Executor executor;
    private MappedStatement mappedStatement;
    private BoundSql boundSql;
    private Configuration configuration;
    private ResultHandler<?> resultHandler;

    private SqlTraceInterceptor interceptor;
    private Object parameter;
    private RowBounds rowBounds;
    private CacheKey cacheKey;
    private List<Integer> result;
    private org.apache.ibatis.plugin.Invocation invocation;

    @BeforeEach
    public void setup() throws Exception {
        // 创建真实对象
        configuration = new Configuration();

        // 使用更规范的SQL语句
        String sql = "SELECT id, name, age FROM users WHERE status = 'active'";

        // 创建BoundSql
        boundSql = new BoundSql(configuration, sql, Collections.emptyList(), new Object());

        // 创建MappedStatement
        mappedStatement = new MappedStatement.Builder(configuration, "testStatement", new StaticSqlSource(configuration, sql), SqlCommandType.SELECT).resultMaps(Collections.singletonList(new ResultMap.Builder(configuration, "resultMap", Integer.class, Collections.emptyList()).build())).build();

        // 模拟executor
        executor = mock(Executor.class);

        // 创建拦截器实例
        interceptor = new SqlTraceInterceptor(true, true, "traceId",20000);

        // 设置初始上下文
        SqlTraceContext.setTraceId("test-trace-id");

        // 初始化测试数据
        parameter = new Object();
        rowBounds = new RowBounds();
        cacheKey = new CacheKey();
        result = new ArrayList<>();
        resultHandler = mock(ResultHandler.class);

        // 设置模拟对象的行为
        when(executor.createCacheKey(any(), any(), any(), any())).thenReturn(cacheKey);
        when(executor.query(any(MappedStatement.class), eq(parameter), eq(rowBounds), eq(resultHandler))).thenReturn(Collections.singletonList(result));
        when(executor.query(any(MappedStatement.class), eq(parameter), eq(rowBounds), eq(resultHandler), eq(cacheKey), any(BoundSql.class))).thenReturn(Collections.singletonList(result));
    }

    @Test
    public void testInterceptWithSelectStatement() throws Throwable {
        // 创建调用对象
        Object[] args = new Object[]{mappedStatement, parameter, rowBounds, resultHandler};
        invocation = new org.apache.ibatis.plugin.Invocation(executor, Executor.class.getDeclaredMethod("query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class), args);

        assertNotNull(interceptor, "Interceptor should not be null");

        // 执行拦截器方法
        Object returnValue = interceptor.intercept(invocation);
        // 验证结果和调用
        assertNotNull(returnValue);

        // 在这里我们不使用精确的验证，因为我们主要测试拦截器是否正常工作
        verify(executor, times(1)).query(any(MappedStatement.class), any(), any(), any());
    }

    @Test
    public void testInterceptWithExtendedSignature() throws Throwable {
        // 创建调用对象，使用扩展的方法签名
        Object[] args = new Object[]{mappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql};
        invocation = new org.apache.ibatis.plugin.Invocation(executor, Executor.class.getDeclaredMethod("query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class), args);

        assertNotNull(interceptor, "Interceptor should not be null");

        // 执行拦截器方法
        Object returnValue = interceptor.intercept(invocation);
        // 验证结果和调用
        assertNotNull(returnValue);
        // 在这里我们不使用精确的验证，因为我们主要测试拦截器是否正常工作
        verify(executor, times(1)).query(any(MappedStatement.class), any(), any(), any(), any(), any(BoundSql.class));
    }
}