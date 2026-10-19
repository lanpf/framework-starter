package com.cloud.framework.starter.message.rabbitmq.delayed.support;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayedRabbitMessageSupportTest {

    @Test
    void shouldSnapshotHeadersBeforeCallbackExecution() {
        Map<String, Object> headers = new LinkedHashMap<>(Map.of("x-source", "original", "x-removed", "retained"));
        MessagePostProcessor processor = DelayedRabbitMessageSupport.headersPostProcessor(headers);

        headers.put("x-source", "changed");
        headers.remove("x-removed");
        headers.put("x-added", "late");

        Message message = processor.postProcessMessage(new Message(new byte[0]));

        assertThat(message.getMessageProperties().getHeaders()).containsOnly(
                Map.entry("x-source", "original"), Map.entry("x-removed", "retained"));
    }

    @Test
    void shouldTreatNullAndInitiallyEmptyHeadersAsAbsent() {
        Map<String, Object> headers = new LinkedHashMap<>();
        MessagePostProcessor processor = DelayedRabbitMessageSupport.headersPostProcessor(headers);
        headers.put("x-added", "late");

        Message fromEmpty = processor.postProcessMessage(new Message(new byte[0]));
        Message fromNull = DelayedRabbitMessageSupport.headersPostProcessor(null)
                .postProcessMessage(new Message(new byte[0]));

        assertThat(fromEmpty.getMessageProperties().getHeaders()).isEmpty();
        assertThat(fromNull.getMessageProperties().getHeaders()).isEmpty();
    }

    @Test
    void shouldKeepSnapshotIndependentOfPreviouslyProcessedMessages() {
        Map<String, Object> headers = new LinkedHashMap<>(Map.of("x-source", "original"));
        MessagePostProcessor processor = DelayedRabbitMessageSupport.headersPostProcessor(headers);
        Message first = processor.postProcessMessage(new Message(new byte[0]));
        first.getMessageProperties().getHeaders().put("x-source", "changed");
        first.getMessageProperties().getHeaders().put("x-added", "late");

        Message second = processor.postProcessMessage(new Message(new byte[0]));

        assertThat(second.getMessageProperties().getHeaders()).containsOnly(Map.entry("x-source", "original"));
        assertThat(headers).containsOnly(Map.entry("x-source", "original"));
    }

    @Test
    void shouldPreserveNullableAndMutableHeaderValues() {
        List<String> value = new ArrayList<>(List.of("original"));
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("x-null", null);
        headers.put("x-value", value);

        Message message = DelayedRabbitMessageSupport.headersPostProcessor(headers)
                .postProcessMessage(new Message(new byte[0]));

        assertThat(message.getMessageProperties().getHeaders()).containsEntry("x-null", null);
        assertThat(message.getMessageProperties().getHeaders().get("x-value")).isSameAs(value);
    }

    @Test
    void shouldRoundDelayUpToTheNextTick() {
        assertEquals(0, DelayedRabbitMessageSupport.ticks(Duration.ZERO, Duration.ofSeconds(1)));
        assertEquals(1, DelayedRabbitMessageSupport.ticks(Duration.ofMillis(1), Duration.ofSeconds(1)));
        assertEquals(2, DelayedRabbitMessageSupport.ticks(Duration.ofMillis(1001), Duration.ofSeconds(1)));
    }

    @Test
    void shouldEncodeTicksAndDestinationInLevelRoutingKey() {
        assertEquals("0.1.0.1.order-timeout", DelayedRabbitMessageSupport.levelRoutingKey(5, 4, "order-timeout"));
    }

    @Test
    void shouldBuildLevelBindingKeysByLevel() {
        assertEquals("1.#", DelayedRabbitMessageSupport.waitingLevelBindingKey(3, 4));
        assertEquals("*.*.0.#", DelayedRabbitMessageSupport.bypassLevelBindingKey(1, 4));
    }

    @Test
    void shouldBuildExactDeliveryBindingKeys() {
        assertEquals("foo.orders", DelayedRabbitMessageSupport.immediateDeliveryBindingKey("foo.orders"));
        assertEquals(
                "*.*.*.*.foo.orders",
                DelayedRabbitMessageSupport.delayedDeliveryBindingKey(4, "foo.orders")
        );
    }
}
