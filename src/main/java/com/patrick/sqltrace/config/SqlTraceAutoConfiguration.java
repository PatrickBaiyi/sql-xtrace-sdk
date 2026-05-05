package com.patrick.sqltrace.config;

import com.patrick.sqltrace.core.SqlTraceInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot自动配置，将拦截器注册到所有SqlSessionFactory
 */
@Configuration
@EnableConfigurationProperties(SqlTraceProperties.class)
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnProperty(prefix = "sql.xtrace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlTraceAutoConfiguration implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SqlTraceAutoConfiguration.class);

    @Autowired
    private SqlTraceProperties properties;

    private volatile SqlTraceInterceptor interceptorInstance;

    private final Set<SqlSessionFactory> processedFactories =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Bean
    public SqlTraceInterceptor sqlTraceInterceptor() {
        if (interceptorInstance == null) {
            interceptorInstance = new SqlTraceInterceptor(properties.getTraceIdFieldName());
            logger.info("SQL追踪拦截器已创建, fieldName={}", properties.getTraceIdFieldName());
        }
        return interceptorInstance;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            SqlSessionFactory factory = (SqlSessionFactory) bean;
            if (processedFactories.add(factory)) {
                factory.getConfiguration().addInterceptor(sqlTraceInterceptor());
                logger.info("SQL追踪拦截器已注册到SqlSessionFactory: {}", beanName);
            }
        }
        return bean;
    }
}
