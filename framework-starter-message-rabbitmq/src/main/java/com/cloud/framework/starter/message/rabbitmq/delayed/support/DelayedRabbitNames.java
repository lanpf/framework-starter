package com.cloud.framework.starter.message.rabbitmq.delayed.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DelayedRabbitNames {
    public static final String DELAYED_PREFIX = "delayed.";
    public static final String DELAYED_LEVEL_EXCHANGE_PREFIX = DELAYED_PREFIX + "level.x.";
    public static final String DELAYED_LEVEL_QUEUE_PREFIX = DELAYED_PREFIX + "level.q.";
    public static final String DELAYED_DELIVERY_EXCHANGE = DELAYED_PREFIX + "delivery.x";
    public static final String DELAYED_DELIVERY_QUEUE_PREFIX = DELAYED_PREFIX + "delivery.q.";
    public static final String DELAYED_DELIVERY_DEAD_LETTER_EXCHANGE = DELAYED_PREFIX + "dlx";
    public static final String DELAYED_DELIVERY_DEAD_LETTER_QUEUE_PREFIX = DELAYED_PREFIX + "dlq.";
}
