package com.cloud.framework.starter.lock.redis.redisson;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedissonLockTest {

    @Test
    void shouldUseWatchdogOverloadWhenAutoRenewalIsEnabled() throws InterruptedException {
        AtomicReference<Object[]> invocationArguments = new AtomicReference<>();
        RLock delegate = rLock(invocationArguments);
        RedissonLock lock = new RedissonLock(delegate, Duration.ofSeconds(30), true);

        assertTrue(lock.tryLock(5, TimeUnit.SECONDS));

        assertEquals(
                Arrays.asList(5L, -1L, TimeUnit.SECONDS),
                Arrays.asList(invocationArguments.get())
        );
    }

    @Test
    void shouldUseExplicitLeaseTimeWhenAutoRenewalIsDisabled() throws InterruptedException {
        AtomicReference<Object[]> invocationArguments = new AtomicReference<>();
        RLock delegate = rLock(invocationArguments);
        long waitTimeNanos = Duration.ofSeconds(5).toNanos();
        long leaseTimeNanos = Duration.ofSeconds(30).toNanos();
        RedissonLock lock = new RedissonLock(delegate, Duration.ofSeconds(30), false);

        assertTrue(lock.tryLock(5, TimeUnit.SECONDS));

        assertEquals(
                Arrays.asList(waitTimeNanos, leaseTimeNanos, TimeUnit.NANOSECONDS),
                Arrays.asList(invocationArguments.get())
        );
    }

    private RLock rLock(AtomicReference<Object[]> invocationArguments) {
        return (RLock) Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class<?>[]{RLock.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("tryLock")) {
                        invocationArguments.set(arguments);
                        return true;
                    }
                    return null;
                }
        );
    }
}
