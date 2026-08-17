package com.cloud.framework.starter.message.rabbitmq.delayed.template;

import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import com.cloud.framework.starter.message.rabbitmq.delayed.DelayedRabbitProperties;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpIllegalStateException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BinaryDelayedRabbitTemplateTest {

    @Test
    void shouldSendToBinaryEntryExchangeWithRoundedDelay() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        BinaryDelayedRabbitTemplate template = template(rabbitTemplate);
        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        template.convertAndSend(
                "orders",
                "order-1",
                payload -> Map.of("x-source", "test"),
                payload -> Duration.ofMillis(4001)
        );

        verify(rabbitTemplate).convertAndSend(
                eq("delayed.level.x.3"),
                eq("0.1.0.1.orders"),
                eq("order-1"),
                postProcessor.capture()
        );
        assertThat(postProcessor.getValue().postProcessMessage(new org.springframework.amqp.core.Message(new byte[0]))
                .getMessageProperties().getHeaders()).containsEntry("x-source", "test");
    }

    @Test
    void shouldSendZeroDelayDirectlyToDeliveryExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        BinaryDelayedRabbitTemplate template = template(rabbitTemplate);

        template.convertAndSend("orders", "order-1", payload -> Duration.ZERO);

        verify(rabbitTemplate).convertAndSend(
                eq("delayed.delivery.x"),
                eq("orders"),
                eq("order-1"),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void shouldRejectUnknownDestinationAndExcessiveDelay() {
        BinaryDelayedRabbitTemplate template = template(mock(RabbitTemplate.class));

        assertThatThrownBy(() -> template.convertAndSend("unknown", "order-1", payload -> Duration.ZERO))
                .isInstanceOf(AmqpIllegalStateException.class);
        assertThatThrownBy(() -> template.convertAndSend("orders", "order-1", payload -> Duration.ofSeconds(16)))
                .isInstanceOf(AmqpIllegalStateException.class);
    }

    private BinaryDelayedRabbitTemplate template(RabbitTemplate rabbitTemplate) {
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.setLevels(4);
        properties.setTickDuration(Duration.ofSeconds(1));
        properties.getDestinations().add("orders");
        return new BinaryDelayedRabbitTemplate(
                rabbitTemplate,
                new RabbitPublisherConfirm(new RabbitMessageProperties()),
                properties
        );
    }
}
