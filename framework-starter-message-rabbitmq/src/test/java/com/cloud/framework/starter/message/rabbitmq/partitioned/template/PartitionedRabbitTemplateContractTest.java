package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.framework.message.PartitionedOperations;
import org.junit.jupiter.api.Test;

class PartitionedRabbitTemplateContractTest {
    @Test
    void nativeRabbitTemplateOwnsTheGenericPartitionedContract() {
        assertThat(PartitionedOperations.class).isAssignableFrom(PartitionedRabbitTemplate.class);
    }
}
