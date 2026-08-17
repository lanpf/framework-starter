package com.cloud.framework.starter.message.rabbitmq.delayed.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.MessagePostProcessor;

import java.time.Duration;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DelayedRabbitMessageSupport {

    public static String levelExchange(Integer level) {
        return DelayedRabbitNames.DELAYED_LEVEL_EXCHANGE_PREFIX + level;
    }

    public static String levelQueue(Integer level) {
        return DelayedRabbitNames.DELAYED_LEVEL_QUEUE_PREFIX + level;
    }

    public static String levelRoutingKey(long ticks, Integer levels, String destination) {
        StringBuilder routingKey = new StringBuilder(levels * 2 + destination.length());
        for (int level = levels - 1; level >= 0; level--) {
            routingKey.append((ticks >>> level) & 1L).append('.');
        }
        return routingKey.append(destination).toString();
    }

    public static String waitingLevelBindingKey(Integer level, Integer levels) {
        return levelBindingKey(level, levels, "1");
    }

    public static String bypassLevelBindingKey(Integer level, Integer levels) {
        return levelBindingKey(level, levels, "0");
    }

    private static String levelBindingKey(Integer level, Integer levels, String branch) {
        int processedLevels = (levels - 1) - level;
        return "*.".repeat(processedLevels) + branch + ".#";
    }

    public static String deliveryExchange() {
        return DelayedRabbitNames.DELAYED_DELIVERY_EXCHANGE;
    }

    public static String deliveryQueue(String destination) {
        return DelayedRabbitNames.DELAYED_DELIVERY_QUEUE_PREFIX + destination;
    }

    public static String deliveryDeadLetterExchange() {
        return DelayedRabbitNames.DELAYED_DELIVERY_DEAD_LETTER_EXCHANGE;
    }

    public static String deliveryDeadLetterQueue(String destination) {
        return DelayedRabbitNames.DELAYED_DELIVERY_DEAD_LETTER_QUEUE_PREFIX + destination;
    }

    public static String immediateDeliveryBindingKey(String destination) {
        return destination;
    }

    public static String delayedDeliveryBindingKey(Integer levels, String destination) {
        return "*.".repeat(levels) + destination;
    }

    public static long ticks(Duration delay, Duration tickDuration) {
        if (delay.isZero()) {
            return 0;
        }
        long completeTicks = delay.dividedBy(tickDuration);
        return delay.minus(tickDuration.multipliedBy(completeTicks)).isZero()
                ? completeTicks
                : Math.addExact(completeTicks, 1);
    }

    public static MessagePostProcessor headersPostProcessor(Map<String, Object> headers) {
        return message -> {
            if (headers != null && !headers.isEmpty()) {
                message.getMessageProperties().setHeaders(headers);
            }
            return message;
        };
    }
}
