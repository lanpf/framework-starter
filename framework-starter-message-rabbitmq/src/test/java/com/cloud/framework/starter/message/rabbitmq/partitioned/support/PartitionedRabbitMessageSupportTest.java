package com.cloud.framework.starter.message.rabbitmq.partitioned.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.framework.message.support.MessageHeaders;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

class PartitionedRabbitMessageSupportTest {
    @Test
    void shouldSnapshotHeadersBeforeCallbackExecution() {
        Map<String, Object> headers = new LinkedHashMap<>(Map.of("x-source", "original", "x-removed", "retained"));
        MessagePostProcessor processor = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, "partition-a");

        headers.put("x-source", "changed");
        headers.remove("x-removed");
        headers.put("x-added", "late");

        Message message = processor.postProcessMessage(new Message(new byte[0]));

        assertThat(message.getMessageProperties().getHeaders()).containsOnly(
                Map.entry("x-source", "original"),
                Map.entry("x-removed", "retained"),
                Map.entry(MessageHeaders.PARTITION_KEY, "partition-a")
        );
        assertThat(headers).doesNotContainKey(MessageHeaders.PARTITION_KEY);
    }

    @Test
    void shouldTreatNullAndInitiallyEmptyHeadersAsAbsent() {
        Map<String, Object> headers = new LinkedHashMap<>();
        MessagePostProcessor processor = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, "partition-a");
        headers.put("x-added", "late");

        Message fromEmpty = processor.postProcessMessage(new Message(new byte[0]));
        Message fromNull = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(null, "partition-a")
                .postProcessMessage(new Message(new byte[0]));

        assertThat(fromEmpty.getMessageProperties().getHeaders()).containsOnly(Map.entry(MessageHeaders.PARTITION_KEY, "partition-a"));
        assertThat(fromNull.getMessageProperties().getHeaders()).isEqualTo(fromEmpty.getMessageProperties().getHeaders());
    }

    @Test
    void shouldKeepSnapshotIndependentOfPreviouslyProcessedMessages() {
        Map<String, Object> headers = new LinkedHashMap<>(Map.of("x-source", "original"));
        MessagePostProcessor processor = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, "partition-a");
        Message first = processor.postProcessMessage(new Message(new byte[0]));
        first.getMessageProperties().getHeaders().put("x-source", "changed");
        first.getMessageProperties().getHeaders().put("x-added", "late");

        Message second = processor.postProcessMessage(new Message(new byte[0]));

        assertThat(second.getMessageProperties().getHeaders()).containsOnly(
                Map.entry("x-source", "original"), Map.entry(MessageHeaders.PARTITION_KEY, "partition-a"));
        assertThat(headers).containsOnly(Map.entry("x-source", "original"));
    }

    @Test
    void shouldPreserveNullableAndMutableHeaderValues() {
        List<String> value = new ArrayList<>(List.of("original"));
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("x-null", null);
        headers.put("x-value", value);

        Message message = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, "partition-a")
                .postProcessMessage(new Message(new byte[0]));

        assertThat(message.getMessageProperties().getHeaders()).containsEntry("x-null", null);
        assertThat(message.getMessageProperties().getHeaders().get("x-value")).isSameAs(value);
    }

    @Test
    void shouldOverrideSuppliedPartitionHeaderWithoutChangingInput() {
        Map<String, Object> headers = new LinkedHashMap<>(Map.of(MessageHeaders.PARTITION_KEY, "supplied"));

        Message message = PartitionedRabbitMessageSupport.partitionHeaderPostProcessor(headers, "partition-a")
                .postProcessMessage(new Message(new byte[0]));

        assertThat(message.getMessageProperties().getHeaders()).containsEntry(MessageHeaders.PARTITION_KEY, "partition-a");
        assertThat(headers).containsEntry(MessageHeaders.PARTITION_KEY, "supplied");
    }
}
