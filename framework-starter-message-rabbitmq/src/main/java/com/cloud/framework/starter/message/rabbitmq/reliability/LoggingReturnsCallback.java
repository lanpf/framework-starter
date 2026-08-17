package com.cloud.framework.starter.message.rabbitmq.reliability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class LoggingReturnsCallback implements RabbitTemplate.ReturnsCallback {
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.warn("Rabbit message returned. exchange={}, routingKey={}, replyCode={}, replyText={}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText());
    }
}
