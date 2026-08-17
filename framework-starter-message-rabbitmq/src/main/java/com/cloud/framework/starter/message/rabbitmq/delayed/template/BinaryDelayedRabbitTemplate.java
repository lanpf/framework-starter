package com.cloud.framework.starter.message.rabbitmq.delayed.template;

import com.cloud.framework.starter.message.rabbitmq.delayed.DelayedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.delayed.support.DelayedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpIllegalStateException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Validated
@RequiredArgsConstructor
public class BinaryDelayedRabbitTemplate implements DelayedRabbitTemplate {

    private final RabbitTemplate rabbitTemplate;

    private final RabbitPublisherConfirm publisherConfirm;

    private final DelayedRabbitTopologyRegistry topologyRegistry;


    @Override
    public void send(String destination, Message message, Duration delay) {
        validateDestination(destination);
        RabbitRoute route = route(destination, delay);
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
    public void sendBatch(String destination, List<Message> messages, Duration delay) {
        validateDestination(destination);
        RabbitRoute route = route(destination, delay);
        this.publisherConfirm.sendBatch(this.rabbitTemplate, operations -> {
            List<CorrelationData> correlationDataList = new ArrayList<>(messages.size());
            messages.forEach(message -> {
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
            Function<? super T, Duration> delaySupplier
    ) {
        validateDestination(destination);
        RabbitRoute route = route(destination, delaySupplier.apply(payload));
        Map<String, Object> headers = headersSupplier.apply(payload);
        this.publisherConfirm.send(
                () -> this.rabbitTemplate.convertAndSend(
                        route.getExchange(),
                        route.getRoutingKey(),
                        payload,
                        DelayedRabbitMessageSupport.headersPostProcessor(headers)
                ),
                correlationData -> this.rabbitTemplate.convertAndSend(
                        route.getExchange(),
                        route.getRoutingKey(),
                        payload,
                        DelayedRabbitMessageSupport.headersPostProcessor(headers),
                        correlationData
                )
        );
    }

    @Override
    public <T> void convertAndSendBatch(
            String destination,
            List<T> payloads,
            Function<? super T, Map<String, Object>> headersSupplier,
            Function<? super T, Duration> delaySupplier
    ) {
        validateDestination(destination);
        this.publisherConfirm.sendBatch(this.rabbitTemplate, operations -> {
            List<CorrelationData> correlationDataList = new ArrayList<>(payloads.size());
            payloads.forEach(payload -> {
                        RabbitRoute route = route(destination, delaySupplier.apply(payload));
                        CorrelationData correlationData = new CorrelationData();
                        correlationDataList.add(correlationData);
                        operations.convertAndSend(
                                route.getExchange(),
                                route.getRoutingKey(),
                                payload,
                                DelayedRabbitMessageSupport.headersPostProcessor(headersSupplier.apply(payload)),
                                correlationData
                        );
                    }
            );
            return correlationDataList;
        });
    }

    private RabbitRoute route(String destination, Duration delay) {
        if (delay == null) {
            throw new AmqpIllegalStateException("Rabbit delayed message delay must not be null.");
        }
        if (delay.isNegative()) {
            throw new AmqpIllegalStateException("Rabbit delayed message delay must not be negative.");
        }
        long ticks;
        try {
            ticks = DelayedRabbitMessageSupport.ticks(delay, this.topologyRegistry.tickDuration());
        } catch (ArithmeticException ex) {
            throw new AmqpIllegalStateException("Rabbit delayed message delay exceeds the supported range.", ex);
        }
        long maxTicks = (1L << this.topologyRegistry.levels()) - 1;
        if (ticks > maxTicks) {
            throw new AmqpIllegalStateException(
                    "Rabbit delayed message delay exceeds the configured maximum of "
                            + this.topologyRegistry.tickDuration().multipliedBy(maxTicks) + "."
            );
        }
        if (ticks == 0) {
            return new RabbitRoute(DelayedRabbitMessageSupport.deliveryExchange(), destination);
        }
        return new RabbitRoute(
                DelayedRabbitMessageSupport.levelExchange(this.topologyRegistry.levels() - 1),
                DelayedRabbitMessageSupport.levelRoutingKey(ticks, this.topologyRegistry.levels(), destination)
        );
    }

    private void validateDestination(String destination) {
        if (!this.topologyRegistry.destinations().contains(destination)) {
            throw new AmqpIllegalStateException(
                    "Rabbit delayed message destination is not configured: " + destination
            );
        }
    }
}
