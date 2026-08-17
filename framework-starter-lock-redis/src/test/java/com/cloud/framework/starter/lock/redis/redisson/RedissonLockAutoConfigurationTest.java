package com.cloud.framework.starter.lock.redis.redisson;

import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import org.junit.jupiter.api.Test;
import org.redisson.config.Config;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedissonLockAutoConfigurationTest {

    @Test
    void shouldAlignWatchdogTimeoutWithLeaseTimeBeforeCreatingClient() {
        RedisLockProperties properties = new RedisLockProperties();
        properties.setLeaseTime(Duration.ofSeconds(45));
        Config config = new Config();

        new RedissonLockAutoConfiguration().redisLockWatchdogCustomizer(properties).customize(config);

        assertEquals(Duration.ofSeconds(45).toMillis(), config.getLockWatchdogTimeout());
    }
}
