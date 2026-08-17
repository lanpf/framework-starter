package com.cloud.framework.starter.outbox;

import com.cloud.framework.message.PartitionedOperations;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.publisher.NoopIntegrationEventPublisher;
import com.cloud.framework.starter.outbox.publisher.PartitionedIntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IntegrationEventOutboxAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IntegrationEventOutboxAutoConfiguration.class));

    @Test
    void usesPartitionedPublisherWhenOperationsIsAvailable() {
        this.contextRunner
                .withBean(PartitionedOperations.class, () -> mock(PartitionedOperations.class))
                .run(context -> assertThat(context.getBean(IntegrationEventPublisher.class))
                        .isInstanceOf(PartitionedIntegrationEventPublisher.class));
    }

    @Test
    void usesNoopPublisherWhenOperationsIsMissing() {
        this.contextRunner.run(context -> assertThat(context.getBean(IntegrationEventPublisher.class))
                .isInstanceOf(NoopIntegrationEventPublisher.class));
    }

    @Test
    void failsFastWhenOperationsIsAmbiguous() {
        this.contextRunner
                .withBean("firstPartitionedOperations", PartitionedOperations.class, () -> mock(PartitionedOperations.class))
                .withBean("secondPartitionedOperations", PartitionedOperations.class, () -> mock(PartitionedOperations.class))
                .run(context -> assertThat(context).hasFailed());
    }
}
