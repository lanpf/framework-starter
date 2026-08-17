package com.cloud.framework.starter.lock.redis.springintegration;

import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringIntegrationLockAutoConfigurationTest {

    @Test
    void shouldConfigureRenewalSchedulerFromProperties() {
        RedisLockProperties properties = properties(true);
        properties.getRenewal().setThreadNamePrefix("order-lock-renewal-");
        properties.getRenewal().getPool().setCoreSize(3);

        ThreadPoolTaskScheduler scheduler = new SpringIntegrationLockAutoConfiguration()
                .redisLockRenewalTaskScheduler(
                        properties,
                        objectProvider(new ThreadPoolTaskSchedulerBuilder())
                );

        assertEquals(3, scheduler.getPoolSize());
        assertEquals("order-lock-renewal-", scheduler.getThreadNamePrefix());
        assertTrue(scheduler.isRemoveOnCancelPolicy());
    }

    @Test
    void shouldEnableRenewalUsingNamespaceAndLeaseTime() throws Exception {
        RedisLockProperties properties = properties(true);
        TaskScheduler scheduler = new ThreadPoolTaskScheduler();

        RedisLockRegistry registry = new SpringIntegrationLockAutoConfiguration().redisLockRegistry(
                redisConnectionFactory(),
                properties,
                objectProvider(scheduler),
                namespaced -> namespaced.getNamespace()
        );

        assertEquals("order-service", field(registry, "registryKey"));
        assertEquals(properties.getLeaseTime().toMillis(), field(registry, "expireAfter"));
        assertSame(scheduler, field(registry, "renewalTaskScheduler"));
    }

    @Test
    void shouldUseFixedLeaseTimeWhenRenewalIsDisabled() throws Exception {
        RedisLockProperties properties = properties(false);

        RedisLockRegistry registry = new SpringIntegrationLockAutoConfiguration().redisLockRegistry(
                redisConnectionFactory(),
                properties,
                objectProvider(null),
                namespaced -> namespaced.getNamespace()
        );

        assertEquals(properties.getLeaseTime().toMillis(), field(registry, "expireAfter"));
        assertNull(field(registry, "renewalTaskScheduler"));
    }

    @Test
    void shouldUseApplicationNameWhenNamespaceIsMissing() throws Exception {
        RedisLockProperties properties = properties(false);
        properties.setNamespace(null);

        RedisLockRegistry registry = new SpringIntegrationLockAutoConfiguration().redisLockRegistry(
                redisConnectionFactory(),
                properties,
                objectProvider(null),
                namespaced -> "order-service"
        );

        assertEquals("order-service", field(registry, "registryKey"));
    }

    private RedisLockProperties properties(boolean autoRenewal) {
        RedisLockProperties properties = new RedisLockProperties();
        properties.setNamespace("order-service");
        properties.setLeaseTime(Duration.ofSeconds(30));
        properties.setAutoRenewal(autoRenewal);
        return properties;
    }

    private RedisConnectionFactory redisConnectionFactory() {
        return (RedisConnectionFactory) Proxy.newProxyInstance(
                RedisConnectionFactory.class.getClassLoader(),
                new Class<?>[]{RedisConnectionFactory.class},
                (proxy, method, arguments) -> null
        );
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> objectProvider(T object) {
        return (ObjectProvider<T>) Proxy.newProxyInstance(
                ObjectProvider.class.getClassLoader(),
                new Class<?>[]{ObjectProvider.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getObject", "getIfAvailable", "getIfUnique" -> object;
                    default -> null;
                }
        );
    }

    private Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
