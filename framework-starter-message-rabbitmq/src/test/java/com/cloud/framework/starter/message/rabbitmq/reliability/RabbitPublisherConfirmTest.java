package com.cloud.framework.starter.message.rabbitmq.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitPublisherConfirmTest {
    @Test
    void preservesNativeSendingWhenReliabilityModeIsMissing() {
        RabbitMessageProperties properties = new RabbitMessageProperties();
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(properties);
        AtomicBoolean nativeSenderCalled = new AtomicBoolean();
        AtomicBoolean confirmSenderCalled = new AtomicBoolean();

        publisherConfirm.send(
                () -> nativeSenderCalled.set(true),
                correlationData -> confirmSenderCalled.set(true)
        );

        assertThat(nativeSenderCalled).isTrue();
        assertThat(confirmSenderCalled).isFalse();
    }

    @Test
    void waitsForPublisherAck() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(confirmProperties());

        publisherConfirm.send(
                () -> {
                },
                correlationData -> correlationData.getFuture()
                        .complete(new CorrelationData.Confirm(true, null))
        );
    }

    @Test
    void rejectsPublisherNack() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(confirmProperties());

        assertThatThrownBy(() -> publisherConfirm.send(
                () -> {
                },
                correlationData -> correlationData.getFuture()
                        .complete(new CorrelationData.Confirm(false, "nack"))
        )).isInstanceOf(AmqpException.class).hasMessageContaining("nack");
    }

    @Test
    void rejectsReturnedMessage() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(confirmProperties());

        assertThatThrownBy(() -> publisherConfirm.send(
                () -> {
                },
                correlationData -> {
                    correlationData.setReturned(new ReturnedMessage(
                            new Message(new byte[0], new MessageProperties()),
                            312,
                            "NO_ROUTE",
                            "exchange",
                            "routing-key"
                    ));
                    correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
                }
        )).isInstanceOf(AmqpException.class).hasMessageContaining("NO_ROUTE");
    }

    @Test
    void rejectsPublisherConfirmTimeout() {
        RabbitMessageProperties properties = confirmProperties();
        properties.getProducer().setConfirmTimeout(Duration.ofMillis(1));
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(properties);

        assertThatThrownBy(() -> publisherConfirm.send(
                () -> {
                },
                correlationData -> {
                }
        )).isInstanceOf(AmqpException.class).hasMessageContaining("timed out");
    }

    @Test
    void waitsForAllBatchPublisherAcks() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(confirmProperties());
        RabbitTemplate rabbitTemplate = scopedRabbitTemplate();

        publisherConfirm.sendBatch(rabbitTemplate, operations -> {
            CorrelationData first = confirmed(true, null);
            CorrelationData second = confirmed(true, null);
            return List.of(first, second);
        });
    }

    @Test
    void rejectsBatchWhenReliabilityModeIsMissingBeforeOpeningChannelScope() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(new RabbitMessageProperties());
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        assertThatThrownBy(() -> publisherConfirm.sendBatch(rabbitTemplate, operations -> List.of()))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("requires publisher-confirm");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsBatchWhenAnyPublisherConfirmIsNack() {
        RabbitPublisherConfirm publisherConfirm = new RabbitPublisherConfirm(confirmProperties());
        RabbitTemplate rabbitTemplate = scopedRabbitTemplate();

        assertThatThrownBy(() -> publisherConfirm.sendBatch(
                rabbitTemplate,
                operations -> List.of(confirmed(true, null), confirmed(false, "nack"))
        )).isInstanceOf(AmqpException.class).hasMessageContaining("nack");
    }

    private RabbitTemplate scopedRabbitTemplate() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });
        return rabbitTemplate;
    }

    private CorrelationData confirmed(boolean ack, String reason) {
        CorrelationData correlationData = new CorrelationData();
        correlationData.getFuture().complete(new CorrelationData.Confirm(ack, reason));
        return correlationData;
    }

    private RabbitMessageProperties confirmProperties() {
        RabbitMessageProperties properties = new RabbitMessageProperties();
        properties.getProducer().setReliabilityMode(
                RabbitMessageProperties.ProducerReliabilityMode.PUBLISHER_CONFIRM
        );
        return properties;
    }
}
