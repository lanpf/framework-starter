package com.cloud.framework.starter.message.rabbitmq.partitioned;

import com.cloud.framework.kernel.error.FrameworkException;
import com.cloud.framework.message.error.MessageError;
import com.cloud.framework.message.support.ConsistentHashMessageQueueSelector;
import com.cloud.framework.message.support.HashMessageQueueSelector;
import com.cloud.framework.message.support.MessageQueueSelector;
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.ConsistentHashPluginPartitionedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.PartitionedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.SelectorPartitionedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.partitioned.topology.ConsistentHashPluginPartitionedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.partitioned.topology.PartitionedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.partitioned.topology.SelectorPartitionedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(
        value = RabbitAutoConfiguration.class,
        name = "com.cloud.framework.starter.message.converter.MessageConverterAutoConfiguration"
)
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(PartitionedRabbitProperties.class)
@Import({
        PartitionedRabbitAutoConfiguration.SelectorRoutingAutoConfiguration.class,
        PartitionedRabbitAutoConfiguration.ConsistentHashPluginRoutingAutoConfiguration.class,
        PartitionedRabbitAutoConfiguration.TopologyInitializationAutoConfiguration.class
})
public class PartitionedRabbitAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "framework.rabbitmq.partitioned",
            name = "routing-mode",
            havingValue = "selector",
            matchIfMissing = true
    )
    @Import({
            SelectorRoutingAutoConfiguration.HashSelectorAutoConfiguration.class,
            SelectorRoutingAutoConfiguration.ConsistentHashSelectorAutoConfiguration.class,
            SelectorRoutingAutoConfiguration.MissingConsistentHashDependencyAutoConfiguration.class
    })
    static class SelectorRoutingAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(AmqpAdmin.class)
        PartitionedRabbitTopologyDeclarer selectorPartitionedRabbitDeclarer(
                AmqpAdmin amqpAdmin,
                PartitionedRabbitTopologyRegistry topologyRegistry
        ) {
            return new SelectorPartitionedRabbitTopologyDeclarer(amqpAdmin, topologyRegistry);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean({RabbitTemplate.class, RabbitPublisherConfirm.class})
        PartitionedRabbitTemplate selectorPartitionedRabbitTemplate(
                RabbitTemplate rabbitTemplate,
                RabbitPublisherConfirm publisherConfirm,
                PartitionedRabbitTopologyRegistry topologyRegistry,
                MessageQueueSelector queueSelector
        ) {
            return new SelectorPartitionedRabbitTemplate(rabbitTemplate, publisherConfirm, topologyRegistry, queueSelector);
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(
                prefix = "framework.rabbitmq.partitioned.selector",
                name = "algorithm",
                havingValue = "hash",
                matchIfMissing = true
        )
        static class HashSelectorAutoConfiguration {

            @Bean
            @ConditionalOnMissingBean
            MessageQueueSelector hashMessageQueueSelector() {
                return new HashMessageQueueSelector();
            }
        }


        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(
                prefix = "framework.rabbitmq.partitioned.selector",
                name = "algorithm",
                havingValue = "consistent-hash"
        )
        @ConditionalOnClass(name = "com.google.common.hash.Hashing")
        static class ConsistentHashSelectorAutoConfiguration {

            @Bean
            @ConditionalOnMissingBean
            MessageQueueSelector consistentHashMessageQueueSelector() {
                return new ConsistentHashMessageQueueSelector();
            }
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(
                prefix = "framework.rabbitmq.partitioned.selector",
                name = "algorithm",
                havingValue = "consistent-hash"
        )
        @ConditionalOnMissingClass("com.google.common.hash.Hashing")
        static class MissingConsistentHashDependencyAutoConfiguration {

            @Bean
            @ConditionalOnMissingBean
            MessageQueueSelector missingConsistentHashDependency() {
                throw new FrameworkException(
                        MessageError.CONFIGURATION_INVALID,
                        "Rabbit partitioned consistent-hash selector requires Guava on the application classpath."
                );
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "framework.rabbitmq.partitioned",
            name = "routing-mode",
            havingValue = "consistent-hash-plugin"
    )
    static class ConsistentHashPluginRoutingAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(AmqpAdmin.class)
        PartitionedRabbitTopologyDeclarer consistentHashPluginPartitionedRabbitDeclarer(
                AmqpAdmin amqpAdmin,
                PartitionedRabbitTopologyRegistry topologyRegistry
        ) {
            return new ConsistentHashPluginPartitionedRabbitTopologyDeclarer(amqpAdmin, topologyRegistry);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean({RabbitTemplate.class, RabbitPublisherConfirm.class})
        PartitionedRabbitTemplate consistentHashPluginPartitionedRabbitTemplate(
                RabbitTemplate rabbitTemplate,
                RabbitPublisherConfirm publisherConfirm,
                PartitionedRabbitTopologyRegistry topologyRegistry
        ) {
            return new ConsistentHashPluginPartitionedRabbitTemplate(rabbitTemplate, publisherConfirm, topologyRegistry);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(TopologyInitializationCondition.class)
    static class TopologyInitializationAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "partitionedRabbitTopologyInitializer")
        RabbitTopologyInitializer partitionedRabbitTopologyInitializer(
                PartitionedRabbitTopologyDeclarer topologyDeclarer,
                PartitionedRabbitTopologyRegistry topologyRegistry
        ) {
            return new RabbitTopologyInitializer(topologyRegistry, topologyDeclarer);
        }
    }

    static class TopologyInitializationCondition extends AnyNestedCondition {

        TopologyInitializationCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(AmqpAdmin.class)
        static class AmqpAdminAvailable {
        }

        @ConditionalOnBean(PartitionedRabbitTopologyDeclarer.class)
        static class TopologyDeclarerAvailable {
        }
    }
}
