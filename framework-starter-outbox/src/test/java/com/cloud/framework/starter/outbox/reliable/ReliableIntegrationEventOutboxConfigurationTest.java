package com.cloud.framework.starter.outbox.reliable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloud.framework.starter.outbox.IntegrationEventOutboxProperties;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class ReliableIntegrationEventOutboxConfigurationTest {

    private final ReliableIntegrationEventOutboxConfiguration configuration =
            new ReliableIntegrationEventOutboxConfiguration();

    @Test
    void bindsRetryableStyleProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(RetryPropertiesConfiguration.class)
                .withPropertyValues(
                        "framework.outbox.integration-event.retry.max-attempts=5",
                        "framework.outbox.integration-event.retry.retry-for[0]=java.lang.IllegalStateException",
                        "framework.outbox.integration-event.retry.no-retry-for[0]=java.lang.IllegalArgumentException",
                        "framework.outbox.integration-event.retry.backoff.delay=10ms",
                        "framework.outbox.integration-event.retry.backoff.max-delay=1s",
                        "framework.outbox.integration-event.retry.backoff.multiplier=2",
                        "framework.outbox.integration-event.retry.backoff.random=true"
                )
                .run(context -> {
                    IntegrationEventOutboxProperties.Retry retry =
                            context.getBean(IntegrationEventOutboxProperties.class).getRetry();
                    assertThat(retry.getMaxAttempts()).isEqualTo(5);
                    assertThat(retry.getRetryFor()).containsExactly(IllegalStateException.class);
                    assertThat(retry.getNoRetryFor()).containsExactly(IllegalArgumentException.class);
                    assertThat(retry.getBackoff().getDelay()).isEqualTo(Duration.ofMillis(10));
                    assertThat(retry.getBackoff().getMaxDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(retry.getBackoff().getMultiplier()).isEqualTo(2.0);
                    assertThat(retry.getBackoff().isRandom()).isTrue();
                });
    }

    @Test
    void bindsSpringBootTaskExecutionPoolProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(RetryPropertiesConfiguration.class)
                .withPropertyValues(
                        "framework.outbox.integration-event.async.thread-name-prefix=outbox-worker-",
                        "framework.outbox.integration-event.async.pool.core-size=2",
                        "framework.outbox.integration-event.async.pool.max-size=6",
                        "framework.outbox.integration-event.async.pool.queue-capacity=100",
                        "framework.outbox.integration-event.async.pool.allow-core-thread-timeout=false",
                        "framework.outbox.integration-event.async.pool.keep-alive=30s",
                        "framework.outbox.integration-event.async.pool.shutdown.accept-tasks-after-context-close=true"
                )
                .run(context -> {
                    IntegrationEventOutboxProperties.Async async =
                            context.getBean(IntegrationEventOutboxProperties.class).getAsync();
                    assertThat(async.getThreadNamePrefix()).isEqualTo("outbox-worker-");
                    assertThat(async.getPool().getCoreSize()).isEqualTo(2);
                    assertThat(async.getPool().getMaxSize()).isEqualTo(6);
                    assertThat(async.getPool().getQueueCapacity()).isEqualTo(100);
                    assertThat(async.getPool().isAllowCoreThreadTimeout()).isFalse();
                    assertThat(async.getPool().getKeepAlive()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(async.getPool().getShutdown().isAcceptTasksAfterContextClose()).isTrue();
                });
    }

    @Test
    void buildsTaskExecutorWithSpringBootBuilder() {
        IntegrationEventOutboxProperties properties = new IntegrationEventOutboxProperties();
        properties.getAsync().setThreadNamePrefix("outbox-worker-");
        properties.getAsync().getPool().setCoreSize(2);
        properties.getAsync().getPool().setMaxSize(6);
        properties.getAsync().getPool().setQueueCapacity(100);
        properties.getAsync().getPool().setKeepAlive(Duration.ofSeconds(30));
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("taskExecutorBuilder", new ThreadPoolTaskExecutorBuilder());

        ThreadPoolTaskExecutor executor = this.configuration.integrationEventOutboxTaskExecutor(
                properties,
                beanFactory.getBeanProvider(ThreadPoolTaskExecutorBuilder.class)
        );

        assertThat(executor.getThreadNamePrefix()).isEqualTo("outbox-worker-");
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(6);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
        assertThat(executor.getKeepAliveSeconds()).isEqualTo(30);
    }

    @Test
    void appliesRetryForAndNoRetryFor() {
        IntegrationEventOutboxProperties properties = propertiesWithShortBackoff();
        properties.getRetry().getRetryFor().add(IllegalStateException.class);
        properties.getRetry().getNoRetryFor().add(IllegalArgumentException.class);
        RetryOperations retryOperations = this.configuration.integrationEventOutboxRetryOperations(properties);
        AtomicInteger retryableAttempts = new AtomicInteger();
        AtomicInteger excludedAttempts = new AtomicInteger();

        assertThatThrownBy(() -> retryOperations.execute(context -> {
            retryableAttempts.incrementAndGet();
            throw new IllegalStateException("retryable");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> retryOperations.execute(context -> {
            excludedAttempts.incrementAndGet();
            throw new IllegalArgumentException("excluded");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(retryableAttempts).hasValue(3);
        assertThat(excludedAttempts).hasValue(1);
    }

    @Test
    void selectsBackoffPolicyFromBackoffProperties() {
        IntegrationEventOutboxProperties fixedProperties = propertiesWithShortBackoff();
        assertThat(backOffPolicy(fixedProperties)).isInstanceOf(FixedBackOffPolicy.class);

        IntegrationEventOutboxProperties uniformProperties = propertiesWithShortBackoff();
        uniformProperties.getRetry().getBackoff().setMaxDelay(Duration.ofMillis(10));
        assertThat(backOffPolicy(uniformProperties)).isInstanceOf(UniformRandomBackOffPolicy.class);

        IntegrationEventOutboxProperties exponentialProperties = propertiesWithShortBackoff();
        exponentialProperties.getRetry().getBackoff().setMaxDelay(Duration.ofMillis(10));
        exponentialProperties.getRetry().getBackoff().setMultiplier(2.0);
        exponentialProperties.getRetry().getBackoff().setRandom(true);
        assertThat(backOffPolicy(exponentialProperties)).isInstanceOf(ExponentialRandomBackOffPolicy.class);
    }

    private Object backOffPolicy(IntegrationEventOutboxProperties properties) {
        RetryTemplate retryTemplate = (RetryTemplate) this.configuration
                .integrationEventOutboxRetryOperations(properties);
        return ReflectionTestUtils.getField(retryTemplate, "backOffPolicy");
    }

    private IntegrationEventOutboxProperties propertiesWithShortBackoff() {
        IntegrationEventOutboxProperties properties = new IntegrationEventOutboxProperties();
        properties.getRetry().getBackoff().setDelay(Duration.ofMillis(1));
        return properties;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IntegrationEventOutboxProperties.class)
    static class RetryPropertiesConfiguration {
    }
}
