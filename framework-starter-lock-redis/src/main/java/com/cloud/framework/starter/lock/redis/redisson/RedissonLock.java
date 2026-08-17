package com.cloud.framework.starter.lock.redis.redisson;

import org.redisson.api.RLock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class RedissonLock implements Lock {

    private final RLock delegate;
    private final long leaseTimeNanos;
    private final boolean autoRenewal;

    public RedissonLock(RLock delegate, Duration leaseTime, boolean autoRenewal) {
        this.delegate = delegate;
        this.leaseTimeNanos = leaseTime.toNanos();
        this.autoRenewal = autoRenewal;
    }

    @Override
    public void lock() {
        if (autoRenewal) {
            delegate.lock();
            return;
        }
        delegate.lock(leaseTimeNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (autoRenewal) {
            delegate.lockInterruptibly();
            return;
        }
        delegate.lockInterruptibly(leaseTimeNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean tryLock() {
        try {
            long leaseTime = autoRenewal ? -1 : leaseTimeNanos;
            return delegate.tryLock(0, leaseTime, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (autoRenewal) {
            return delegate.tryLock(time, -1, unit);
        }
        return delegate.tryLock(unit.toNanos(time), leaseTimeNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void unlock() {
        delegate.unlock();
    }

    @Override
    public Condition newCondition() {
        return delegate.newCondition();
    }
}
