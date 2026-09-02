package com.cloud.framework.starter.persistence.mybatisplus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cloud.framework.persistence.PersistenceProperties;
import com.cloud.framework.persistence.naming.PersistenceTableNaming;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class PersistenceMybatisPlusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "framework.persistence")
    public PersistenceProperties persistenceProperties() {
        return new PersistenceProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<InnerInterceptor> innerInterceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        innerInterceptors.orderedStream().forEach(interceptor::addInnerInterceptor);
        return interceptor;
    }

    @Bean(name = "paginationInnerInterceptor")
    @ConditionalOnMissingBean(name = "paginationInnerInterceptor")
    public InnerInterceptor paginationInnerInterceptor(PersistenceProperties properties) {
        DbType dbType = DbType.getDbType(properties.getDatabase());
        if (dbType == DbType.OTHER) {
            throw new IllegalArgumentException("Unsupported framework.persistence.database: " + properties.getDatabase());
        }
        return new PaginationInnerInterceptor(dbType);
    }

    @Bean(name = "dynamicTableNameInnerInterceptor")
    @ConditionalOnMissingBean(name = "dynamicTableNameInnerInterceptor")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public InnerInterceptor dynamicTableNameInnerInterceptor(PersistenceProperties properties) {
        return new DynamicTableNameInnerInterceptor(
                (sql, tableName) -> PersistenceTableNaming.apply(
                        tableName,
                        properties.getNaming().getTablePrefix(),
                        properties.getNaming().getTableSuffix()
                )
        );
    }
}
