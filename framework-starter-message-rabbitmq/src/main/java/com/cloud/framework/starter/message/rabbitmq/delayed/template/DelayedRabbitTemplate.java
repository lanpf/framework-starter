package com.cloud.framework.starter.message.rabbitmq.delayed.template;

import com.cloud.framework.message.DelayedOperations;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.core.Message;

import java.time.Duration;
import java.util.List;

public interface DelayedRabbitTemplate extends DelayedOperations {

    void send(@NotBlank String destination, @NotNull Message message, @NotNull Duration delay);

    void sendBatch(@NotBlank String destination, @NotEmpty List<Message> messages, @NotNull Duration delay);
}
