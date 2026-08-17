package com.cloud.framework.starter.message.rabbitmq.partitioned;

import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyRegistry;
import jakarta.validation.constraints.NotBlank;

public interface PartitionedRabbitTopologyRegistry extends RabbitTopologyRegistry {

    Integer partitions(@NotBlank String destination);

    Integer weight(@NotBlank String destination);

    boolean deadLetterEnabled();
}
