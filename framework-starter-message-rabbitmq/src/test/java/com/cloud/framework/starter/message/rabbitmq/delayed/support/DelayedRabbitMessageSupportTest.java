package com.cloud.framework.starter.message.rabbitmq.delayed.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayedRabbitMessageSupportTest {

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
