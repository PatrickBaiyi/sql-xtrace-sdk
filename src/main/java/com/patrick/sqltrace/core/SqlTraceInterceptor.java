package com.patrick.sqltrace.core;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 * MyBatis拦截器，拦截所有查询并注入traceId列
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlTraceInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(SqlTraceInterceptor.class);

    private static final String[] MDC_KEYS = {"midea-apm-traceid", "tid", "traceId"};

    private final TraceIdSqlParser sqlParser;

    public SqlTraceInterceptor(String traceIdFieldName) {
        this.sqlParser = new TraceIdSqlParser(traceIdFieldName);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String traceId = resolveTraceId();
        if (traceId == null) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
        Executor executor = (Executor) invocation.getTarget();

        BoundSql boundSql;
        CacheKey cacheKey;

        if (args.length == 4) {
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }

        String originalSql = boundSql.getSql();
        String modifiedSql = sqlParser.parseAndModify(originalSql, traceId);

        if (originalSql.equals(modifiedSql)) {
            return invocation.proceed();
        }

        BoundSql newBoundSql = copyBoundSql(ms, boundSql, modifiedSql);
        MappedStatement newMs = copyMappedStatement(ms, newBoundSql);

        if (args.length == 4) {
            return executor.query(newMs, parameter, rowBounds, resultHandler);
        } else {
            return executor.query(newMs, parameter, rowBounds, resultHandler, cacheKey, newBoundSql);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private String resolveTraceId() {
        for (String key : MDC_KEYS) {
            String value = MDC.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        String ctxValue = SqlTraceContext.getTraceId();
        return ctxValue.isEmpty() ? null : ctxValue;
    }

    private BoundSql copyBoundSql(MappedStatement ms, BoundSql original, String newSql) {
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), newSql,
                original.getParameterMappings(), original.getParameterObject());
        try {
            Field field = BoundSql.class.getDeclaredField("additionalParameters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) field.get(original);
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    newBoundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            logger.warn("复制BoundSql additionalParameters失败", e);
        }
        return newBoundSql;
    }

    private MappedStatement copyMappedStatement(MappedStatement ms, BoundSql newBoundSql) {
        SqlSource sqlSource = parameterObject -> newBoundSql;
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId(), sqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }
}
