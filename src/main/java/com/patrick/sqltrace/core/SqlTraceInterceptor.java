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
 * MyBatis拦截器，拦截所有查询并使用解析器修改SQL。它处理了BoundSql和MappedStatement的复制和修改过程。
 *
 * @author patrickz
 * @since 2025-03-12
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlTraceInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(SqlTraceInterceptor.class);

    private final TraceIdSqlParser sqlParser;

    public SqlTraceInterceptor(boolean enableCountQueries, boolean enableGroupByQueries,
                               String traceIdFieldName, int maxPageSize) {
        this.sqlParser = new TraceIdSqlParser(enableCountQueries, enableGroupByQueries,
                traceIdFieldName, maxPageSize);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // 从上下文获取当前的追踪ID
        String traceId = MDC.get("midea-apm-traceid");
        if (traceId == null || traceId.isEmpty()) {
            traceId = MDC.get("tid");
            if (traceId == null || traceId.isEmpty()) {
                traceId = MDC.get("traceId");
                if (traceId == null || traceId.isEmpty()) {
                    traceId = SqlTraceContext.getTraceId();
                    if (traceId == null || traceId.isEmpty()) {
                        // 没有可用的追踪ID，使用原始SQL继续
                        return invocation.proceed();
                    }
                }
            }
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];

        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql;

        // 根据方法签名获取BoundSql
        if (args.length == 4) {
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }

        // 解析并修改SQL
        String originalSql = boundSql.getSql();

        String modifiedSql = sqlParser.parseAndModify(originalSql, traceId);

        // 如果SQL没有被修改，使用原始SQL继续
        if (originalSql.equals(modifiedSql)) {
            return invocation.proceed();
        }

        // 使用修改后的SQL创建新的MappedStatement
        BoundSql newBoundSql = createNewBoundSql(ms, boundSql, modifiedSql);
        MappedStatement newMs = copyMappedStatementWithNewSqlSource(ms, new BoundSqlSqlSource(newBoundSql));

        // 使用新的MappedStatement进行查询
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
        // 本实现不使用此方法
    }

    /**
     * 使用修改后的SQL创建新的BoundSql
     */
    /**
     * 使用修改后的SQL创建新的BoundSql
     */
    private BoundSql createNewBoundSql(MappedStatement ms, BoundSql boundSql, String modifiedSql) {
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), modifiedSql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());

        // 复制additionalParameters到新的BoundSql
        try {
            Field additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            additionalParametersField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Object> additionalParameters = (Map<String, Object>) additionalParametersField.get(boundSql);

            if (additionalParameters != null) {
                for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                    newBoundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            // 如果反射失败，尝试继续工作
            logger.warn("无法从原始BoundSql复制additionalParameters", e);
        }

        return newBoundSql;
    }
//    private BoundSql createNewBoundSql(MappedStatement ms, BoundSql boundSql, String modifiedSql) {
//        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), modifiedSql,
//                boundSql.getParameterMappings(), boundSql.getParameterObject());
//
//        // 使用反射复制metaParameters
//        try {
//            Field metaParamsField = BoundSql.class.getDeclaredField("metaParameters");
//            metaParamsField.setAccessible(true);
//
//            Map<String, Object> metaParams = (Map<String, Object>) metaParamsField.get(boundSql);
//            if (metaParams != null) {
//                for (Map.Entry<String, Object> entry : metaParams.entrySet()) {
//                    newBoundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
//                }
//            }
//        } catch (Exception e) {
//            // 如果反射失败，尝试继续工作
//            logger.warn("无法从原始BoundSql复制metaParameters", e);
//        }
//
//        return newBoundSql;
//    }

    /**
     * 使用新的SqlSource创建MappedStatement的副本
     */
    private MappedStatement copyMappedStatementWithNewSqlSource(MappedStatement ms, SqlSource newSqlSource) {
        try {
            Field field = ms.getClass().getDeclaredField("sqlSource");
            field.setAccessible(true);

            MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(),
                    newSqlSource, ms.getSqlCommandType());

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
        } catch (Exception e) {
            logger.error("创建新的MappedStatement时出错", e);
            return ms;
        }
    }

    /**
     * 返回特定BoundSql的SqlSource实现
     */
    static class BoundSqlSqlSource implements SqlSource {
        private final BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}