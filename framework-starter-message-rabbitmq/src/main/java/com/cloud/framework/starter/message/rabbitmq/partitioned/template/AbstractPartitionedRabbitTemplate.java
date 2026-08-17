package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpIllegalStateException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Validated
@RequiredArgsConstructor
public abstract class AbstractPartitionedRabbitTemplate implements PartitionedRabbitTemplate {
    private final RabbitTemplate rabbitTemplate;

    private final RabbitPublisherConfirm publisherConfirm;

    protected final PartitionedRabbitTopologyRegistry topologyRegistry;

    protected abstract RabbitRoute route(String destination, Object partitionArg);

    @Override
    public void send(String destination, Message message, Object partitionArg) {
        validateDestination(destination);
        RabbitRoute route = route(destination, partitionArg);
        PartitionedRabbitMessageSupport.setPartitionHeader(message, partitionArg);
        this.publisherConfirm.send(
                () -> this.rabbitTemplate.send(
                        route.getExchange(),
                        route.getRoutingKey(),
                        message
                ),
                correlationData -> this.rabbitTemplate.send(
                        route.getExchange(),
                        route.getRoutingKey(),
                        message,
                        correlationData
                )
        );
    }

    @Override
    public void sendBatch(
            String destination,
            List<Message> messages,
            Object partitionArg
    ) {
        validateDestination(destination);
        RabbitRoute route = route(destination, partitionArg);
        this.publisherConfirm.sendBatch(this.rabbitTemplate, operations -> {
            List<CorrelationData> correlationDataList = new ArrayList<>(messages.size());
            messages.forEach(message -> {
                PartitionedRabbitMessageSupport.setPartitionHeader(message, partitionArg);
                CorrelationData correlationData = new CorrelationData();
                correlationDataList.add(correlationData);
                operations.send(
                        route.getExchange(),
                        route.getRoutingKey(),
                        message,
                        correlationData
                );
            });
            return correlationDataList;
        });
    }

    @Override
    public <T> void convertAndSend(
            String destination,
            T payload,
            Function<? super T, Map<String, Object>> headersSupplier,
            Function<? super T, ?> partitionArgSupplier) {
        validateDestination(destination);

        Object partitionArg = partitionArgSupplier.apply(payload);
        Map<String, Object> headers = headersSupplier.apply(payload);

        RabbitRoute route = route(destination, partitionArg);
        this.publisherConfirm.send(
                () -> this.rabbitTemplate.convertAndSend(
                        route.getExchange(),
                        route.getRoutingKey(),
                        payload,
                        PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, partitionArg)
                ),
                correlationData -> this.rabbitTemplate.convertAndSend(
                        route.getExchange(),
                        route.getRoutingKey(),
                        payload,
                        PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, partitionArg),
                        correlationData
                )
        );
    }

    @Override
    public <T> void convertAndSendBatch(
            String destination,
            List<T> payloads,
            Function<? super T, Map<String, Object>> headersSupplier,
            Function<? super T, ?> partitionArgSupplier
    ) {
        validateDestination(destination);
        this.publisherConfirm.sendBatch(this.rabbitTemplate, operations -> {
            List<CorrelationData> correlationDataList = new ArrayList<>(payloads.size());
            payloads.forEach(payload -> {
                Object partitionArg = partitionArgSupplier.apply(payload);
                Map<String, Object> headers = headersSupplier.apply(payload);

                RabbitRoute route = route(destination, partitionArg);
                CorrelationData correlationData = new CorrelationData();
                correlationDataList.add(correlationData);
                operations.convertAndSend(
                        route.getExchange(),
                        route.getRoutingKey(),
                        payload,
                        PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, partitionArg),
                        correlationData
                );
            });
            return correlationDataList;
        });
    }

    private void validateDestination(String destination) {
        if (!this.topologyRegistry.destinations().contains(destination)) {
            throw new AmqpIllegalStateException(
                    "Rabbit partitioned message destination is not configured: " + destination
            );
        }
    }
}
