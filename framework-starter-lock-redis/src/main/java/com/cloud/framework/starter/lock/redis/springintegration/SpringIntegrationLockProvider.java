package com.cloud.framework.starter.lock.redis.springintegration;

import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.lock.LockProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public class SpringIntegrationLockProvider implements LockProvider {

    private final LockRegistry lockRegistry;
    private final ResourceNameResolver resourceNameResolver;

    @Override
    public Lock obtain(String... lockNames) {
        return lockRegistry.obtain(resourceNameResolver.resolve(lockNames));
    }
}
