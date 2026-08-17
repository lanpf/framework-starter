package com.cloud.framework.starter.message.rabbitmq.delayed.topology;

import com.cloud.framework.starter.message.rabbitmq.delayed.DelayedRabbitProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BinaryDelayedRabbitTopologyDeclarerTest {

    @Test
    void shouldDeclareBinaryLevelsAndDestinationTopology() {
        AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.setLevels(2);
        properties.setTickDuration(Duration.ofMillis(100));
        BinaryDelayedRabbitTopologyDeclarer declarer = new BinaryDelayedRabbitTopologyDeclarer(amqpAdmin, properties);

        declarer.initialize();
        declarer.declare("orders");

        ArgumentCaptor<Queue> queues = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin, times(4)).declareQueue(queues.capture());
        List<Queue> declaredQueues = queues.getAllValues();
        assertThat(declaredQueues).extracting(Queue::getName)
                .containsExactly(
                        "delayed.level.q.1",
                        "delayed.level.q.0",
                        "delayed.delivery.q.orders",
                        "delayed.dlq.orders"
                );
        assertThat(declaredQueues.get(0).getArguments())
                .containsEntry("x-message-ttl", 200L)
                .containsEntry("x-dead-letter-exchange", "delayed.level.x.0")
                .containsEntry("x-dead-letter-strategy", "at-least-once")
                .containsEntry("x-overflow", "reject-publish");
        assertThat(declaredQueues.get(1).getArguments())
                .containsEntry("x-message-ttl", 100L)
                .containsEntry("x-dead-letter-exchange", "delayed.delivery.x");

        ArgumentCaptor<Exchange> exchanges = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin, times(4)).declareExchange(exchanges.capture());
        assertThat(exchanges.getAllValues()).extracting(Exchange::getName)
                .containsExactly(
                        "delayed.delivery.x",
                        "delayed.dlx",
                        "delayed.level.x.1",
                        "delayed.level.x.0"
                );

        InOrder declarationOrder = inOrder(amqpAdmin);
        declarationOrder.verify(amqpAdmin).declareExchange(argThat(exchange ->
                "delayed.delivery.x".equals(exchange.getName())));
        declarationOrder.verify(amqpAdmin).declareExchange(argThat(exchange ->
                "delayed.dlx".equals(exchange.getName())));
        declarationOrder.verify(amqpAdmin).declareExchange(argThat(exchange ->
                "delayed.level.x.1".equals(exchange.getName())));
        declarationOrder.verify(amqpAdmin).declareExchange(argThat(exchange ->
                "delayed.level.x.0".equals(exchange.getName())));
        declarationOrder.verify(amqpAdmin).declareQueue(any(Queue.class));
    }
}
