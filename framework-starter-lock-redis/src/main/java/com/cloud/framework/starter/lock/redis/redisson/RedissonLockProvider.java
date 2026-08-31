package com.cloud.framework.starter.lock.redis.redisson;

import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public class RedissonLockProvider implements LockProvider {

    private final RedissonClient redissonClient;
    private final ResourceNameResolver resourceNameResolver;
    private final RedisLockProperties properties;

    @Override
    public Lock obtain(String... lockNames) {
        return new RedissonLock(
                redissonClient.getLock(resourceNameResolver.resolve(lockNames)),
                properties.getLeaseTime(),
                properties.isAutoRenewal()
        );
    }
}
