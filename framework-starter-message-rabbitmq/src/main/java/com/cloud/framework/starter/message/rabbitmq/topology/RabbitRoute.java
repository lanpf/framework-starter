package com.cloud.framework.starter.message.rabbitmq.topology;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RabbitRoute {
    private final String exchange;

    private final String routingKey;
}
