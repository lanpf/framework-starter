package com.cloud.framework.starter.outbox;

import com.cloud.framework.message.PartitionedOperations;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.direct.DirectIntegrationEventOutboxConfiguration;
import com.cloud.framework.starter.outbox.publisher.NoopIntegrationEventPublisher;
import com.cloud.framework.starter.outbox.publisher.PartitionedIntegrationEventPublisher;
import com.cloud.framework.starter.outbox.reliable.ReliableIntegrationEventOutboxConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(IntegrationEventOutboxProperties.class)
@Import({
        DirectIntegrationEventOutboxConfiguration.class,
        ReliableIntegrationEventOutboxConfiguration.class
})
public class IntegrationEventOutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventPublisher integrationEventPublisher(ObjectProvider<PartitionedOperations> partitionedOperations) {
        PartitionedOperations operations = partitionedOperations.getIfAvailable();
        if (operations == null) {
            return new NoopIntegrationEventPublisher();
        }
        return new PartitionedIntegrationEventPublisher(operations);
    }
}
