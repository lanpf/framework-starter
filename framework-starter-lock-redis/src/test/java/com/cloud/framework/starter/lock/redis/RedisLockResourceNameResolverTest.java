package com.cloud.framework.starter.lock.redis;

import com.cloud.framework.core.naming.NamespaceResolver;
import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.starter.lock.redis.redisson.RedissonLockAutoConfiguration;
import com.cloud.framework.starter.lock.redis.springintegration.SpringIntegrationLockAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisLockResourceNameResolverTest {

    @Test
    void shouldPassThroughLockNameForSpringIntegration() {
        ResourceNameResolver resolver = new SpringIntegrationLockAutoConfiguration().lockResourceNameResolver();

        assertEquals("create-order:1001", resolver.resolve("create-order:1001"));
    }

    @Test
    void shouldExplicitlyApplyNamespaceForRedisson() {
        RedisLockProperties properties = new RedisLockProperties();
        properties.setNamespace("order-service");
        ResourceNameResolver resolver = new RedissonLockAutoConfiguration().lockResourceNameResolver(
                properties,
                namespaced -> namespaced.getNamespace()
        );

        assertEquals("order-service:create-order:1001", resolver.resolve("create-order:1001"));
    }

    @Test
    void shouldApplyApplicationNameForRedissonWhenNamespaceIsMissing() {
        ResourceNameResolver resolver = new RedissonLockAutoConfiguration().lockResourceNameResolver(
                new RedisLockProperties(),
                namespaced -> "order-service"
        );

        assertEquals("order-service:create-order:1001", resolver.resolve("create-order:1001"));
    }

    @Test
    void shouldUseProvidedNamespaceResolverForRedisson() {
        NamespaceResolver namespaceResolver = namespaced -> "shared-service";
        ResourceNameResolver resolver = new RedissonLockAutoConfiguration().lockResourceNameResolver(
                new RedisLockProperties(),
                namespaceResolver
        );

        assertEquals("shared-service:create-order:1001", resolver.resolve("create-order:1001"));
    }
}
