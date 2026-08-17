package com.cloud.framework.starter.message.rabbitmq.reliability;

import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@ConditionalOnClass({RabbitTemplate.class, CachingConnectionFactory.class})
@EnableConfigurationProperties(RabbitMessageProperties.class)
@Import({
        RabbitReliabilityAutoConfiguration.ProducerTransactionConfiguration.class,
        RabbitReliabilityAutoConfiguration.PublisherConfirmConfiguration.class,
        RabbitReliabilityAutoConfiguration.ConsumerTransactionConfiguration.class
})
public class RabbitReliabilityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "framework.rabbitmq.producer",
            name = "reliability-mode",
            havingValue = "transaction"
    )
    static class ProducerTransactionConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "rabbitProducerReliabilityBeanPostProcessor")
        static BeanPostProcessor rabbitProducerReliabilityBeanPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof CachingConnectionFactory connectionFactory) {
                        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.NONE);
                        connectionFactory.setPublisherReturns(false);
                    }
                    return bean;
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean(PlatformTransactionManager.class)
        RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
            return new RabbitTransactionManager(connectionFactory);
        }

        @Bean
        @ConditionalOnMissingBean(name = "rabbitReliabilityTemplateCustomizer")
        RabbitTemplateCustomizer rabbitReliabilityTemplateCustomizer() {
            return rabbitTemplate -> rabbitTemplate.setChannelTransacted(true);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "framework.rabbitmq.producer",
            name = "reliability-mode",
            havingValue = "publisher-confirm"
    )
    static class PublisherConfirmConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "rabbitProducerReliabilityBeanPostProcessor")
        static BeanPostProcessor rabbitProducerReliabilityBeanPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof CachingConnectionFactory connectionFactory) {
                        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
                        connectionFactory.setPublisherReturns(true);
                    }
                    return bean;
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean(name = "rabbitReliabilityTemplateCustomizer")
        RabbitTemplateCustomizer rabbitReliabilityTemplateCustomizer(
                ObjectProvider<RabbitTemplate.ConfirmCallback> confirmCallback,
                ObjectProvider<RabbitTemplate.ReturnsCallback> returnsCallback
        ) {
            return rabbitTemplate -> {
                rabbitTemplate.setMandatory(true);
                rabbitTemplate.setConfirmCallback(confirmCallback.getIfUnique(LoggingConfirmCallback::new));
                rabbitTemplate.setReturnsCallback(returnsCallback.getIfUnique(LoggingReturnsCallback::new));
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "framework.rabbitmq.consumer",
            name = "reliability-mode",
            havingValue = "transaction"
    )
    static class ConsumerTransactionConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "rabbitConsumerTransactionBeanPostProcessor")
        static BeanPostProcessor rabbitConsumerTransactionBeanPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof AbstractRabbitListenerContainerFactory<?> listenerContainerFactory) {
                        listenerContainerFactory.setChannelTransacted(true);
                        listenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.AUTO);
                    }
                    return bean;
                }
            };
        }
    }
}
