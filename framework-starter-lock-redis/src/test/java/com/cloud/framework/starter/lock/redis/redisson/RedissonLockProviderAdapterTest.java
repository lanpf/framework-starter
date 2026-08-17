package com.cloud.framework.starter.lock.redis.redisson;

import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RedissonLockProviderAdapterTest {

    @Test
    void shouldObtainLockUsingTheCanonicalPhysicalKey() {
        AtomicReference<String> obtainedLockName = new AtomicReference<>();
        RLock delegate = rLock();
        RedissonClient redissonClient = redissonClient(obtainedLockName, delegate);
        RedisLockProperties properties = new RedisLockProperties();
        ResourceNameResolver resourceNameResolver = new RedissonLockAutoConfiguration()
                .lockResourceNameResolver(
                        properties,
                        namespaced -> "order-service"
                );
        LockProvider provider = new RedissonLockProviderAdapter(
                redissonClient,
                resourceNameResolver,
                properties
        );

        Lock lock = provider.obtain("create-order:1001");

        assertInstanceOf(RedissonLock.class, lock);
        assertEquals("order-service:create-order:1001", obtainedLockName.get());
    }

    private RedissonClient redissonClient(AtomicReference<String> obtainedLockName, RLock lock) {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getLock")) {
                        obtainedLockName.set((String) arguments[0]);
                        return lock;
                    }
                    return null;
                }
        );
    }

    private RLock rLock() {
        return (RLock) Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class<?>[]{RLock.class},
                (proxy, method, arguments) -> null
        );
    }
}
