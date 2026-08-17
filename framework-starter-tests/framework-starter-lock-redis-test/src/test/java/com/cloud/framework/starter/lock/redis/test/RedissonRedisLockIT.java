package com.cloud.framework.starter.lock.redis.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.cloud.framework.lock.LockContext;
import com.cloud.framework.lock.LockExecutor;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.redisson.RedissonLockProviderAdapter;
import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@Testcontainers
class RedissonRedisLockIT {

    private static final Duration AUTO_RENEWAL_LEASE_TIME = Duration.ofSeconds(1);
    private static final Duration FIXED_LEASE_TIME = Duration.ofSeconds(1);
    private static final Duration LOCK_WAIT_TIME = Duration.ofMillis(150);
    private static final Duration OWNER_TIMEOUT = Duration.ofSeconds(10);

    @Container
    private static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:8.4.4"));

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    @AfterEach
    void closeContexts() {
        for (int index = this.contexts.size() - 1; index >= 0; index--) {
            this.contexts.get(index).close();
        }
        this.contexts.clear();
    }

    @Test
    void shouldProvideMutualExclusionAndAutomaticRenewal() throws Exception {
        String applicationName = "redisson-auto-renewal-lock-test";
        String expectedKey = applicationName + ":create-order:1001";
        ConfigurableApplicationContext ownerContext = startContext(
                applicationName,
                AUTO_RENEWAL_LEASE_TIME,
                true
        );
        ConfigurableApplicationContext contenderContext = startContext(
                applicationName,
                AUTO_RENEWAL_LEASE_TIME,
                true
        );
        assertThat(ownerContext.getBean(LockProvider.class)).isInstanceOf(RedissonLockProviderAdapter.class);

        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService ownerExecutor = Executors.newSingleThreadExecutor();
        Future<Boolean> owner = ownerExecutor.submit(() -> ownerContext.getBean(LockExecutor.class).execute(
                new LockContext("create-order", "1001"),
                () -> {
                    acquired.countDown();
                    if (!release.await(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release Redis lock");
                    }
                }
        ));

        try {
            assertThat(acquired.await(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            StringRedisTemplate redisTemplate = ownerContext.getBean(StringRedisTemplate.class);
            assertKeyWithPositiveTtl(redisTemplate, expectedKey, AUTO_RENEWAL_LEASE_TIME);

            await().pollInterval(Duration.ofMillis(100))
                    .during(AUTO_RENEWAL_LEASE_TIME.plusMillis(500))
                    .atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertKeyWithPositiveTtl(
                            redisTemplate,
                            expectedKey,
                            AUTO_RENEWAL_LEASE_TIME
                    ));

            Optional<String> blocked = contenderContext.getBean(LockExecutor.class).execute(
                    new LockContext("create-order", "1001", LOCK_WAIT_TIME),
                    () -> "contender"
            );
            assertThat(blocked).isEmpty();
            log.info("Observed Redisson renewing lock: key={}, ttl={}ms",
                    expectedKey, redisTemplate.getExpire(expectedKey, TimeUnit.MILLISECONDS));

            release.countDown();
            assertThat(owner.get(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(redisTemplate.hasKey(expectedKey)).isFalse());

            Optional<String> acquiredAfterRelease = contenderContext.getBean(LockExecutor.class).execute(
                    new LockContext("create-order", "1001", LOCK_WAIT_TIME),
                    () -> "contender"
            );
            assertThat(acquiredAfterRelease).contains("contender");
        }
        finally {
            release.countDown();
            ownerExecutor.shutdownNow();
        }
    }

    @Test
    void shouldReleaseTheResourceWhenFixedLeaseExpires() throws Exception {
        String applicationName = "redisson-fixed-lease-lock-test";
        String expectedKey = applicationName + ":settle-order:2001";
        ConfigurableApplicationContext ownerContext = startContext(applicationName, FIXED_LEASE_TIME, false);
        ConfigurableApplicationContext contenderContext = startContext(applicationName, FIXED_LEASE_TIME, false);
        assertThat(ownerContext.getBean(LockProvider.class)).isInstanceOf(RedissonLockProviderAdapter.class);

        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService ownerExecutor = Executors.newSingleThreadExecutor();
        Future<Boolean> owner = ownerExecutor.submit(() -> ownerContext.getBean(LockExecutor.class).execute(
                new LockContext("settle-order", "2001"),
                () -> {
                    acquired.countDown();
                    if (!release.await(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IllegalStateException("Timed out waiting to finish expired Redis lock owner");
                    }
                }
        ));

        try {
            assertThat(acquired.await(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            StringRedisTemplate redisTemplate = ownerContext.getBean(StringRedisTemplate.class);
            assertKeyWithPositiveTtl(redisTemplate, expectedKey, FIXED_LEASE_TIME);
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(redisTemplate.hasKey(expectedKey)).isFalse());

            Optional<String> acquiredAfterExpiry = contenderContext.getBean(LockExecutor.class).execute(
                    new LockContext("settle-order", "2001", LOCK_WAIT_TIME),
                    () -> "contender"
            );
            assertThat(acquiredAfterExpiry).contains("contender");
            log.info("Observed Redisson fixed lock expiry: key={}, leaseTime={}ms",
                    expectedKey, FIXED_LEASE_TIME.toMillis());

            release.countDown();
            assertThatThrownBy(() -> owner.get(OWNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                    .isInstanceOf(ExecutionException.class);
        }
        finally {
            release.countDown();
            ownerExecutor.shutdownNow();
        }
    }

    private ConfigurableApplicationContext startContext(
            String applicationName,
            Duration leaseTime,
            boolean autoRenewal
    ) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties(applicationName, leaseTime, autoRenewal))
                .run();
        this.contexts.add(context);
        return context;
    }

    private Map<String, Object> properties(String applicationName, Duration leaseTime, boolean autoRenewal) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.application.name", applicationName);
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", false);
        properties.put("spring.data.redis.host", REDIS.getRedisHost());
        properties.put("spring.data.redis.port", REDIS.getRedisPort());
        properties.put("spring.data.redis.timeout", "2s");
        properties.put("framework.lock.redis.provider", "redisson");
        properties.put("framework.lock.redis.lease-time", leaseTime);
        properties.put("framework.lock.redis.auto-renewal", autoRenewal);
        return properties;
    }

    private void assertKeyWithPositiveTtl(
            StringRedisTemplate redisTemplate,
            String expectedKey,
            Duration leaseTime
    ) {
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        Long ttl = redisTemplate.getExpire(expectedKey, TimeUnit.MILLISECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(leaseTime.toMillis());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}
