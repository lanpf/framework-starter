package com.cloud.framework.starter.message.rabbitmq.reliability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class LoggingConfirmCallback implements RabbitTemplate.ConfirmCallback {
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.debug("Rabbit message confirmed. correlationData={}", correlationData);
            return;
        }
        log.warn("Rabbit message confirm failed. correlationData={}, cause={}", correlationData, cause);
    }
}
