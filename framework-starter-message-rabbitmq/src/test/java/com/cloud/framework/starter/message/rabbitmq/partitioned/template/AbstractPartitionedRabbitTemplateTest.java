package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitProperties;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import java.util.List;
import java.util.Map;

import com.cloud.framework.starter.message.rabbitmq.topology.RabbitRoute;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class AbstractPartitionedRabbitTemplateTest {
    @Test
    void sendsPayloadBatchInOneChannelScopeWithPerPayloadRouting() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(operations).convertAndSend(
                anyString(),
                anyString(),
                any(),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        TestPartitionedRabbitTemplate template = new TestPartitionedRabbitTemplate(
                rabbitTemplate,
                new RabbitPublisherConfirm(confirmProperties())
        );
        BatchPayload first = new BatchPayload("first", "partition-a");
        BatchPayload second = new BatchPayload("second", "partition-b");

        template.convertAndSendBatch(
                "orders",
                List.of(first, second),
                payload -> Map.of("x-name", payload.getName()),
                BatchPayload::getPartitionArg
        );

        verify(rabbitTemplate).invoke(any());
        InOrder publishOrder = inOrder(operations);
        publishOrder.verify(operations).convertAndSend(
                eq("partitioned.x.orders"),
                eq("partition-a"),
                eq(first),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
        publishOrder.verify(operations).convertAndSend(
                eq("partitioned.x.orders"),
                eq("partition-b"),
                eq(second),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    private RabbitMessageProperties confirmProperties() {
        RabbitMessageProperties properties = new RabbitMessageProperties();
        properties.getProducer().setReliabilityMode(
                RabbitMessageProperties.ProducerReliabilityMode.PUBLISHER_CONFIRM
        );
        return properties;
    }

    private static class TestPartitionedRabbitTemplate extends AbstractPartitionedRabbitTemplate {
        TestPartitionedRabbitTemplate(RabbitTemplate rabbitTemplate, RabbitPublisherConfirm publisherConfirm) {
            super(rabbitTemplate, publisherConfirm, topologyRegistry());
        }

        @Override
        protected RabbitRoute route(String destination, Object partitionArg) {
            return new RabbitRoute(
                    PartitionedRabbitMessageSupport.exchange(destination),
                    partitionArg.toString()
            );
        }
    }

    private static PartitionedRabbitProperties topologyRegistry() {
        PartitionedRabbitProperties properties = new PartitionedRabbitProperties();
        properties.getDestinations().put("orders", 2);
        return properties;
    }

    @Getter
    @AllArgsConstructor
    private static class BatchPayload {
        private final String name;

        private final String partitionArg;
    }
}
