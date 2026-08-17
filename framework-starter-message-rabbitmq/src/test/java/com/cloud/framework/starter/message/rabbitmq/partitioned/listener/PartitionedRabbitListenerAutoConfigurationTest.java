package com.cloud.framework.starter.message.rabbitmq.partitioned.listener;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.DirectRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartitionedRabbitListenerAutoConfigurationTest {

    private final PartitionedRabbitListenerAutoConfiguration.SimpleContainerConfiguration simpleConfiguration =
            new PartitionedRabbitListenerAutoConfiguration.SimpleContainerConfiguration();

    private final PartitionedRabbitListenerAutoConfiguration.DirectContainerConfiguration directConfiguration =
            new PartitionedRabbitListenerAutoConfiguration.DirectContainerConfiguration();

    @Test
    void shouldApplyOrderingConstraintsAfterSimpleContainerCustomizer() {
        PartitionedRabbitProperties properties = properties(3);
        ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> customizerProvider = customizerProvider(
                (SimpleMessageListenerContainer container) -> {
                    container.setMaxConcurrentConsumers(4);
                    container.setConcurrentConsumers(2);
                    container.setPrefetchCount(10);
                    container.setDefaultRequeueRejected(true);
                }
        );

        SimpleMessageListenerContainer container = this.simpleConfiguration
                .simplePartitionedRabbitListenerContainerFactory(
                        mock(ConnectionFactory.class),
                        new SimpleRabbitListenerContainerFactoryConfigurer(new RabbitProperties()),
                        properties,
                        customizerProvider
                )
                .createListenerContainer(endpoint("simple"));

        assertThat(ReflectionTestUtils.getField(container, "concurrentConsumers")).isEqualTo(1);
        assertThat(ReflectionTestUtils.getField(container, "maxConcurrentConsumers")).isEqualTo(1);
        assertThat(ReflectionTestUtils.getField(container, "prefetchCount")).isEqualTo(3);
        assertThat(ReflectionTestUtils.getField(container, "defaultRequeueRejected")).isEqualTo(false);
    }

    @Test
    void shouldApplyOrderingConstraintsAfterDirectContainerCustomizer() {
        PartitionedRabbitProperties properties = properties(3);
        ObjectProvider<ContainerCustomizer<DirectMessageListenerContainer>> customizerProvider = customizerProvider(
                (DirectMessageListenerContainer container) -> {
                    container.setConsumersPerQueue(2);
                    container.setPrefetchCount(10);
                    container.setDefaultRequeueRejected(true);
                }
        );

        DirectMessageListenerContainer container = this.directConfiguration
                .directPartitionedRabbitListenerContainerFactory(
                        mock(ConnectionFactory.class),
                        new DirectRabbitListenerContainerFactoryConfigurer(new RabbitProperties()),
                        properties,
                        customizerProvider
                )
                .createListenerContainer(endpoint("direct"));

        assertThat(ReflectionTestUtils.getField(container, "consumersPerQueue")).isEqualTo(1);
        assertThat(ReflectionTestUtils.getField(container, "prefetchCount")).isEqualTo(3);
        assertThat(ReflectionTestUtils.getField(container, "defaultRequeueRejected")).isEqualTo(false);
    }

    private PartitionedRabbitProperties properties(Integer prefetch) {
        PartitionedRabbitProperties properties = new PartitionedRabbitProperties();
        properties.getListener().setPrefetch(prefetch);
        return properties;
    }

    private RabbitListenerEndpoint endpoint(String id) {
        RabbitListenerEndpoint endpoint = mock(RabbitListenerEndpoint.class);
        when(endpoint.getId()).thenReturn(id);
        return endpoint;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractMessageListenerContainer> ObjectProvider<ContainerCustomizer<T>> customizerProvider(
            ContainerCustomizer<T> customizer
    ) {
        ObjectProvider<ContainerCustomizer<T>> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            Consumer<ContainerCustomizer<T>> consumer = invocation.getArgument(0);
            consumer.accept(customizer);
            return null;
        }).when(provider).ifUnique(any());
        return provider;
    }
}
