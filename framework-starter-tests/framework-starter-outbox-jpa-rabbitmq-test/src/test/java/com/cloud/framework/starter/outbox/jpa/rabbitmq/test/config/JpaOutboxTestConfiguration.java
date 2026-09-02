package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.config;

import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaPersistenceMapper;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaPersistenceRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.mapstruct.IntegrationEventOutboxJpaMapStructMapper;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.support.PausableAsyncTaskExecutor;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.reliable.ReliableIntegrationEventOutboxConfiguration;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = IntegrationEventOutboxDO.class)
@EnableJpaRepositories(basePackageClasses = IntegrationEventOutboxJpaRepository.class)
public class JpaOutboxTestConfiguration {
    @Bean
    public IntegrationEventOutboxJpaPersistenceMapper integrationEventOutboxJpaPersistenceMapper() {
        return Mappers.getMapper(IntegrationEventOutboxJpaMapStructMapper.class);
    }

    @Bean
    public IntegrationEventOutboxEnvelopePersistenceRepository integrationEventOutboxPersistenceRepository(
            IntegrationEventOutboxJpaRepository repository,
            IntegrationEventOutboxJpaPersistenceMapper mapper
    ) {
        return new IntegrationEventOutboxJpaPersistenceRepository(repository, mapper);
    }

    @Bean(name = ReliableIntegrationEventOutboxConfiguration.TASK_EXECUTOR_BEAN_NAME, destroyMethod = "close")
    public PausableAsyncTaskExecutor integrationEventOutboxTaskExecutor() {
        return new PausableAsyncTaskExecutor();
    }
}
