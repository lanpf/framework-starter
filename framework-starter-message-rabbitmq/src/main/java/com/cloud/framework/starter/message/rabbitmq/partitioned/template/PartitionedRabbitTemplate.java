package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import com.cloud.framework.message.PartitionedOperations;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.core.Message;

import java.util.List;

public interface PartitionedRabbitTemplate extends PartitionedOperations {

    void send(@NotBlank String destination, @NotNull Message message, @NotNull Object partitionArg);

    void sendBatch(@NotBlank String destination, @NotEmpty List<Message> messages, @NotNull Object partitionArg);
}
