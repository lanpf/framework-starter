package com.cloud.framework.starter.message.rabbitmq.partitioned.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PartitionedRabbitNames {
    public static final String PARTITIONED_PREFIX = "partitioned.";
    public static final String PARTITIONED_EXCHANGE_PREFIX = PARTITIONED_PREFIX + "x.";
    public static final String PARTITIONED_QUEUE_PREFIX = PARTITIONED_PREFIX + "q.";
    public static final String PARTITIONED_DEAD_LETTER_EXCHANGE_PREFIX = PARTITIONED_PREFIX + "dlx.";
    public static final String PARTITIONED_DEAD_LETTER_QUEUE_PREFIX = PARTITIONED_PREFIX + "dlq.";
}
