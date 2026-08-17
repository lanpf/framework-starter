package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.IntegrationEventOutboxProperties;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.mapstruct.IntegrationEventOutboxPersistenceMapperConfiguration;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@Import(IntegrationEventOutboxPersistenceMapperConfiguration.class)
@ConditionalOnProperty(prefix = "framework.outbox.integration-event", name = "mode", havingValue = "reliable")
public class ReliableIntegrationEventOutboxConfiguration {
    public static final String TASK_EXECUTOR_BEAN_NAME = "integrationEventOutboxTaskExecutor";
    public static final String RETRY_OPERATIONS_BEAN_NAME = "integrationEventOutboxRetryOperations";

    @Bean(name = TASK_EXECUTOR_BEAN_NAME)
    @ConditionalOnMissingBean(name = TASK_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor integrationEventOutboxTaskExecutor(
            IntegrationEventOutboxProperties properties,
            ObjectProvider<ThreadPoolTaskExecutorBuilder> taskExecutorBuilder
    ) {
        IntegrationEventOutboxProperties.Async async = properties.getAsync();
        TaskExecutionProperties.Pool pool = async.getPool();
        return taskExecutorBuilder.getIfUnique(ThreadPoolTaskExecutorBuilder::new)
                .queueCapacity(pool.getQueueCapacity())
                .corePoolSize(pool.getCoreSize())
                .maxPoolSize(pool.getMaxSize())
                .allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout())
                .keepAlive(pool.getKeepAlive())
                .acceptTasksAfterContextClose(pool.getShutdown().isAcceptTasksAfterContextClose())
                .threadNamePrefix(async.getThreadNamePrefix())
                .build();
    }

    @Bean(name = RETRY_OPERATIONS_BEAN_NAME)
    @ConditionalOnMissingBean(name = RETRY_OPERATIONS_BEAN_NAME)
    public RetryOperations integrationEventOutboxRetryOperations(IntegrationEventOutboxProperties properties) {
        IntegrationEventOutboxProperties.Retry retry = properties.getRetry();
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(createRetryPolicy(retry));
        retryTemplate.setBackOffPolicy(createBackOffPolicy(retry.getBackoff()));
        return retryTemplate;
    }

    private RetryPolicy createRetryPolicy(IntegrationEventOutboxProperties.Retry retry) {
        if (retry.getRetryFor().isEmpty() && retry.getNoRetryFor().isEmpty()) {
            return new SimpleRetryPolicy(retry.getMaxAttempts());
        }
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new LinkedHashMap<>();
        retry.getRetryFor().forEach(exceptionType -> retryableExceptions.put(exceptionType, true));
        retry.getNoRetryFor().forEach(exceptionType -> retryableExceptions.put(exceptionType, false));
        boolean retryNotExcluded = retry.getRetryFor().isEmpty();
        return new SimpleRetryPolicy(
                retry.getMaxAttempts(),
                retryableExceptions,
                true,
                retryNotExcluded
        );
    }

    private BackOffPolicy createBackOffPolicy(IntegrationEventOutboxProperties.Backoff backoff) {
        return BackOffPolicyBuilder.newBuilder()
                .delay(backoff.getDelay().toMillis())
                .maxDelay(backoff.getMaxDelay().toMillis())
                .multiplier(backoff.getMultiplier())
                .random(backoff.isRandom())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventOutbox reliableIntegrationEventOutbox(
            IntegrationEventOutboxPersistenceRepository repository,
            IntegrationEventOutboxPersistenceMapper mapper,
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<Clock> clockProvider
    ) {
        return new ReliableIntegrationEventOutbox(repository, mapper, eventPublisher, resolveClock(clockProvider));
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventOutboxPublisher integrationEventOutboxPublisher(
            IntegrationEventOutboxPersistenceRepository repository,
            IntegrationEventOutboxPersistenceMapper mapper,
            IntegrationEventPublisher integrationEventPublisher,
            ObjectProvider<Clock> clockProvider
    ) {
        return new IntegrationEventOutboxPublisher(
                repository,
                mapper,
                integrationEventPublisher,
                resolveClock(clockProvider)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventOutboxSignalListener integrationEventOutboxSignalListener(
            IntegrationEventOutboxPublisher outboxPublisher,
            @Qualifier(TASK_EXECUTOR_BEAN_NAME) AsyncTaskExecutor taskExecutor,
            @Qualifier(RETRY_OPERATIONS_BEAN_NAME) RetryOperations retryOperations
    ) {
        return new IntegrationEventOutboxSignalListener(outboxPublisher, taskExecutor, retryOperations);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventOutboxFallbackPublisher integrationEventOutboxFallbackPublisher(
            IntegrationEventOutboxPersistenceRepository repository,
            IntegrationEventOutboxPublisher outboxPublisher,
            ObjectProvider<Clock> clockProvider
    ) {
        return new IntegrationEventOutboxFallbackPublisher(repository, outboxPublisher, resolveClock(clockProvider));
    }

    private Clock resolveClock(ObjectProvider<Clock> clockProvider) {
        return clockProvider.getIfAvailable(Clock::systemUTC);
    }
}
