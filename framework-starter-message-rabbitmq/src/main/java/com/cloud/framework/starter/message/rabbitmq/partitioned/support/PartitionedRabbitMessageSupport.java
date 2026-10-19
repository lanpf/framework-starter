package com.cloud.framework.starter.message.rabbitmq.partitioned.support;

import com.cloud.framework.message.support.MessageHeaders;
import com.cloud.framework.message.support.PartitionKeyResolver;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PartitionedRabbitMessageSupport {
    public static String exchange(String destination) {
        return PartitionedRabbitNames.PARTITIONED_EXCHANGE_PREFIX + destination;
    }

    public static String queue(String destination, Integer index) {
        return PartitionedRabbitNames.PARTITIONED_QUEUE_PREFIX + destination + "." + index;
    }

    public static String routingKey(Integer index) {
        return String.valueOf(index);
    }

    public static String deadLetterExchange(String destination) {
        return PartitionedRabbitNames.PARTITIONED_DEAD_LETTER_EXCHANGE_PREFIX + destination;
    }

    public static String deadLetterQueue(String destination, Integer index) {
        return PartitionedRabbitNames.PARTITIONED_DEAD_LETTER_QUEUE_PREFIX + destination + "." + index;
    }


    public static String indexBindingKey(Integer index) {
        return String.valueOf(index);
    }

    public static String weightBindingKey(Integer weight) {
        return String.valueOf(weight);
    }

    public static boolean isPartitionedExchange(String name) {
        return StringUtils.hasText(name) && name.startsWith(PartitionedRabbitNames.PARTITIONED_EXCHANGE_PREFIX);
    }

    public static boolean isPartitionedQueue(String name) {
        return StringUtils.hasText(name) && name.startsWith(PartitionedRabbitNames.PARTITIONED_QUEUE_PREFIX);
    }

    public static void setPartitionHeader(Message message, Object partitionArg) {
        message.getMessageProperties().setHeader(MessageHeaders.PARTITION_KEY, PartitionKeyResolver.resolve(partitionArg));
    }

    /**
     * Captures a shallow snapshot before applying the partition header.
     *
     * @param headers additional headers; null is equivalent to an empty map; mutable values remain shared
     * @param partitionArg the partition argument that overrides any supplied partition header
     * @return a reusable processor independent of subsequent input-map changes
     */
    public static MessagePostProcessor partitionHeaderPostProcessor(Map<String, Object> headers, Object partitionArg) {
        Map<String, Object> headersSnapshot = headers == null
                ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        return message -> {
            message.getMessageProperties().setHeaders(headersSnapshot);
            setPartitionHeader(message, partitionArg);
            return message;
        };
    }
}
