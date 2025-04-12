package com.patrick.sqltrace.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.patrick.sqltrace.core.SqlTraceInterceptor;

import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * Spring Boot自动配置类，负责初始化拦截器并将其添加到所有可用的SqlSessionFactory中。
 *
 * @author zhangjh39
 * @since 2025-03-12
 */
@Configuration
@EnableConfigurationProperties(SqlTraceProperties.class)
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnProperty(prefix = "sql.xtrace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlTraceAutoConfiguration implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SqlTraceAutoConfiguration.class);

    @Autowired
    private SqlTraceProperties properties;

    // 单例拦截器实例
    private SqlTraceInterceptor interceptorInstance;
    
    // 追踪已处理的SqlSessionFactory，防止重复添加拦截器
    private final Set<SqlSessionFactory> processedFactories = new HashSet<>();

    /**
     * 创建SQL追踪拦截器bean
     * @return SqlTraceInterceptor
     */
    @Bean
    public SqlTraceInterceptor sqlTraceInterceptor() {
        if (interceptorInstance == null) {
            interceptorInstance = new SqlTraceInterceptor(
                    properties.isEnableCountQueries(),
                    properties.isEnableGroupByQueries(),
                    properties.getTraceIdFieldName(),
                    properties.getMaxPageSize()
            );
            logger.info("已创建SQL追踪拦截器实例: {}", interceptorInstance);
        }
        return interceptorInstance;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) bean;
            
            // 检查是否已处理过该SqlSessionFactory
            if (!processedFactories.contains(sqlSessionFactory)) {
                // 确保使用单例的拦截器实例
                sqlSessionFactory.getConfiguration().addInterceptor(sqlTraceInterceptor());
                processedFactories.add(sqlSessionFactory);
                logger.info("已将SQL追踪拦截器添加到SqlSessionFactory: {}", sqlSessionFactory);
            } else {
                logger.debug("该SqlSessionFactory已添加过SQL追踪拦截器，跳过: {}", sqlSessionFactory);
            }
        }
        return bean;
    }
}