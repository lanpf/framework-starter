package com.cloud.framework.starter.lock.redis.redisson;

import com.cloud.framework.core.naming.NamespaceResolver;
import com.cloud.framework.core.naming.NamespacedResourceNameResolver;
import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.cloud.framework.starter.lock.redis.RedisLockAutoConfiguration.LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RedissonClient.class, RedissonAutoConfigurationCustomizer.class})
@ConditionalOnProperty(prefix = "framework.lock.redis", name = "provider", havingValue = "redisson")
public class RedissonLockAutoConfiguration {

    @Bean(name = LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(name = LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME)
    public ResourceNameResolver lockResourceNameResolver(
            RedisLockProperties properties,
            NamespaceResolver namespaceResolver
    ) {
        return new NamespacedResourceNameResolver(namespaceResolver, properties);
    }

    @Bean
    public RedissonAutoConfigurationCustomizer redisLockWatchdogCustomizer(
            RedisLockProperties properties
    ) {
        return config -> config.setLockWatchdogTimeout(properties.getLeaseTime().toMillis());
    }

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider redissonLockProvider(
            RedissonClient redissonClient,
            @Qualifier(LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME) ResourceNameResolver resourceNameResolver,
            RedisLockProperties properties
    ) {
        validateWatchdogTimeout(redissonClient, properties);
        return new RedissonLockProviderAdapter(redissonClient, resourceNameResolver, properties);
    }

    private void validateWatchdogTimeout(RedissonClient redissonClient, RedisLockProperties properties) {
        long watchdogTimeout = redissonClient.getConfig().getLockWatchdogTimeout();
        long leaseTime = properties.getLeaseTime().toMillis();
        if (watchdogTimeout != leaseTime) {
            throw new IllegalStateException(
                    "Redisson lockWatchdogTimeout must match framework.lock.redis.lease-time"
            );
        }
    }
}
