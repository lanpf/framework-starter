package com.cloud.framework.starter.message.rabbitmq.topology;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@FunctionalInterface
public interface RabbitTopologyDeclarer {

    default void initialize() {
    }

    void declare(
            @NotBlank
            @Pattern(
                    regexp = RabbitTopologyRegistry.DESTINATION_PATTERN,
                    message = "must not contain Rabbit topic wildcards '*' or '#'"
            )
            String destination
    );
}
