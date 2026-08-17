package com.cloud.framework.starter.message.rabbitmq.delayed;

import com.cloud.framework.starter.message.rabbitmq.delayed.template.BinaryDelayedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.delayed.template.DelayedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.delayed.topology.BinaryDelayedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.delayed.topology.DelayedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(
        value = RabbitAutoConfiguration.class,
        name = "com.cloud.framework.starter.message.converter.MessageConverterAutoConfiguration"
)
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(DelayedRabbitProperties.class)
public class DelayedRabbitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AmqpAdmin.class)
    public DelayedRabbitTopologyDeclarer binaryDelayedRabbitDeclarer(
            AmqpAdmin amqpAdmin,
            DelayedRabbitTopologyRegistry topologyRegistry
    ) {
        return new BinaryDelayedRabbitTopologyDeclarer(amqpAdmin, topologyRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(name = "delayedRabbitTopologyInitializer")
    @ConditionalOnBean(DelayedRabbitTopologyDeclarer.class)
    public RabbitTopologyInitializer delayedRabbitTopologyInitializer(
            DelayedRabbitTopologyDeclarer topologyDeclarer,
            DelayedRabbitTopologyRegistry topologyRegistry
    ) {
        return new RabbitTopologyInitializer(topologyRegistry, topologyDeclarer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RabbitTemplate.class, RabbitPublisherConfirm.class})
    public DelayedRabbitTemplate binaryDelayedRabbitTemplate(
            RabbitTemplate rabbitTemplate,
            RabbitPublisherConfirm publisherConfirm,
            DelayedRabbitTopologyRegistry topologyRegistry
    ) {
        return new BinaryDelayedRabbitTemplate(rabbitTemplate, publisherConfirm, topologyRegistry);
    }
}
