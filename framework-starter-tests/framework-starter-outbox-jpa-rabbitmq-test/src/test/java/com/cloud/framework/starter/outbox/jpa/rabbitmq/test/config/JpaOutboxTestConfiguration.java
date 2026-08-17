package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.config;

import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaPersistenceRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.support.PausableAsyncTaskExecutor;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import com.cloud.framework.starter.outbox.reliable.ReliableIntegrationEventOutboxConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = IntegrationEventOutboxDO.class)
@EnableJpaRepositories(basePackageClasses = IntegrationEventOutboxJpaRepository.class)
public class JpaOutboxTestConfiguration {
    @Bean
    public IntegrationEventOutboxPersistenceRepository integrationEventOutboxPersistenceRepository(
            IntegrationEventOutboxJpaRepository repository
    ) {
        return new IntegrationEventOutboxJpaPersistenceRepository(repository);
    }

    @Bean(name = ReliableIntegrationEventOutboxConfiguration.TASK_EXECUTOR_BEAN_NAME, destroyMethod = "close")
    public PausableAsyncTaskExecutor integrationEventOutboxTaskExecutor() {
        return new PausableAsyncTaskExecutor();
    }
}
