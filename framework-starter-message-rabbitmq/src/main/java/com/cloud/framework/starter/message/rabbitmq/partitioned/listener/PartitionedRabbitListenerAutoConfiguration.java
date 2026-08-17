package com.cloud.framework.starter.message.rabbitmq.partitioned.listener;

import com.cloud.framework.message.support.MessageConverterNames;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitAutoConfiguration;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitProperties;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.DirectRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.validation.Validator;

@AutoConfiguration
@AutoConfigureAfter(
        value = {RabbitAutoConfiguration.class, PartitionedRabbitAutoConfiguration.class},
        name = "com.cloud.framework.starter.message.converter.MessageConverterAutoConfiguration"
)
@ConditionalOnClass({ConnectionFactory.class, RabbitListenerEndpointRegistry.class})
@Import({
        PartitionedRabbitListenerAutoConfiguration.SimpleContainerConfiguration.class,
        PartitionedRabbitListenerAutoConfiguration.DirectContainerConfiguration.class
})
public class PartitionedRabbitListenerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "partitionedRabbitListenerAnnotationBeanPostProcessor")
    @ConditionalOnBean(RabbitListenerEndpointRegistry.class)
    public static PartitionedRabbitListenerAnnotationBeanPostProcessor partitionedRabbitListenerAnnotationBeanPostProcessor(
            ObjectProvider<RabbitListenerEndpointRegistry> endpointRegistry,
            @Qualifier(MessageConverterNames.DEFAULT) ObjectProvider<MessageConverter> messageConverter,
            ObjectProvider<ConversionService> conversionService,
            ObjectProvider<Validator> validator,
            ObjectProvider<PartitionedRabbitTopologyRegistry> topologyRegistry,
            @Qualifier("partitionedRabbitTopologyInitializer") ObjectProvider<RabbitTopologyInitializer> topologyInitializer
    ) {
        return new PartitionedRabbitListenerAnnotationBeanPostProcessor(
                endpointRegistry,
                messageConverter,
                conversionService,
                validator,
                topologyRegistry,
                topologyInitializer
        );
    }

    private static void configureFailureHandling(
            AbstractRabbitListenerContainerFactory<?> factory,
            PartitionedRabbitProperties properties
    ) {
        if (properties.getDeadLetter().isEnabled()) {
            factory.setDefaultRequeueRejected(false);
        }
    }

    private static void configureFailureHandling(
            AbstractMessageListenerContainer container,
            PartitionedRabbitProperties properties
    ) {
        if (properties.getDeadLetter().isEnabled()) {
            container.setDefaultRequeueRejected(false);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(ConnectionFactory.class)
    @ConditionalOnProperty(name = "spring.rabbitmq.listener.type", havingValue = "simple", matchIfMissing = true)
    static class SimpleContainerConfiguration {

        @Bean(name = PartitionedRabbitListenerAnnotationBeanPostProcessor.PARTITIONED_CONTAINER_FACTORY_BEAN_NAME)
        @ConditionalOnMissingBean(name = PartitionedRabbitListenerAnnotationBeanPostProcessor.PARTITIONED_CONTAINER_FACTORY_BEAN_NAME)
        SimpleRabbitListenerContainerFactory simplePartitionedRabbitListenerContainerFactory(
                ConnectionFactory connectionFactory,
                SimpleRabbitListenerContainerFactoryConfigurer configurer,
                PartitionedRabbitProperties properties,
                ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> simpleContainerCustomizer
        ) {
            SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
            configurer.configure(factory, connectionFactory);
            simpleContainerCustomizer.ifUnique(customizer -> factory.setContainerCustomizer(container -> {
                customizer.configure(container);
                configureSimplePartitionedContainer(container, properties);
                configureFailureHandling(container, properties);
            }));
            configureSimplePartitionedFactory(factory, properties);
            configureFailureHandling(factory, properties);
            return factory;
        }

        private void configureSimplePartitionedFactory(
                SimpleRabbitListenerContainerFactory factory,
                PartitionedRabbitProperties properties
        ) {
            factory.setConcurrentConsumers(1);
            factory.setMaxConcurrentConsumers(1);
            factory.setPrefetchCount(properties.getListener().getPrefetch());
        }

        private void configureSimplePartitionedContainer(
                SimpleMessageListenerContainer container,
                PartitionedRabbitProperties properties
        ) {
            container.setConcurrentConsumers(1);
            container.setMaxConcurrentConsumers(1);
            container.setPrefetchCount(properties.getListener().getPrefetch());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(ConnectionFactory.class)
    @ConditionalOnProperty(name = "spring.rabbitmq.listener.type", havingValue = "direct")
    static class DirectContainerConfiguration {

        @Bean(name = PartitionedRabbitListenerAnnotationBeanPostProcessor.PARTITIONED_CONTAINER_FACTORY_BEAN_NAME)
        @ConditionalOnMissingBean(name = PartitionedRabbitListenerAnnotationBeanPostProcessor.PARTITIONED_CONTAINER_FACTORY_BEAN_NAME)
        DirectRabbitListenerContainerFactory directPartitionedRabbitListenerContainerFactory(
                ConnectionFactory connectionFactory,
                DirectRabbitListenerContainerFactoryConfigurer configurer,
                PartitionedRabbitProperties properties,
                ObjectProvider<ContainerCustomizer<DirectMessageListenerContainer>> directContainerCustomizer
        ) {
            DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
            configurer.configure(factory, connectionFactory);
            directContainerCustomizer.ifUnique(customizer -> factory.setContainerCustomizer(container -> {
                customizer.configure(container);
                configureDirectPartitionedContainer(container, properties);
                configureFailureHandling(container, properties);
            }));
            configureDirectPartitionedFactory(factory, properties);
            configureFailureHandling(factory, properties);
            return factory;
        }

        private void configureDirectPartitionedFactory(
                DirectRabbitListenerContainerFactory factory,
                PartitionedRabbitProperties properties
        ) {
            factory.setConsumersPerQueue(1);
            factory.setPrefetchCount(properties.getListener().getPrefetch());
        }

        private void configureDirectPartitionedContainer(
                DirectMessageListenerContainer container,
                PartitionedRabbitProperties properties
        ) {
            container.setConsumersPerQueue(1);
            container.setPrefetchCount(properties.getListener().getPrefetch());
        }
    }
}
