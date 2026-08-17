package com.cloud.framework.starter.lock.redis;

import com.cloud.framework.lock.DefaultLockExecutor;
import com.cloud.framework.lock.LockExecutor;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.redisson.RedissonLockAutoConfiguration;
import com.cloud.framework.starter.lock.redis.springintegration.SpringIntegrationLockAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(name = "com.cloud.framework.starter.autoconfigure.naming.NamespaceAutoConfiguration")
@AutoConfigureBefore(name = {
        "org.redisson.spring.starter.RedissonAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
@Import({SpringIntegrationLockAutoConfiguration.class, RedissonLockAutoConfiguration.class})
public class RedisLockAutoConfiguration {

    public static final String LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME = "lockResourceNameResolver";

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "framework.lock.redis")
    public RedisLockProperties redisLockProperties() {
        return new RedisLockProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockExecutor lockExecutor(LockProvider lockProvider) {
        return new DefaultLockExecutor(lockProvider);
    }
}
