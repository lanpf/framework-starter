package com.cloud.framework.starter.message.rabbitmq.partitioned;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.framework.message.support.ConsistentHashMessageQueueSelector;
import com.cloud.framework.message.support.HashMessageQueueSelector;
import com.cloud.framework.message.support.MessageQueueSelector;
import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.PartitionedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.partitioned.topology.PartitionedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class PartitionedRabbitAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PartitionedRabbitAutoConfiguration.class));

    @Test
    void usesHashSelectorByDefault() {
        this.contextRunner.run(context -> assertThat(context.getBean(MessageQueueSelector.class))
                .isInstanceOf(HashMessageQueueSelector.class));
    }

    @Test
    void usesConsistentHashSelectorWhenConfigured() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.partitioned.selector.algorithm=consistent-hash")
                .run(context -> {
                    MessageQueueSelector selector = context.getBean(MessageQueueSelector.class);

                    assertThat(selector).isInstanceOf(ConsistentHashMessageQueueSelector.class);
                    assertThat(selector.select(4, "aggregate-1")).isBetween(0, 3);
                });
    }

    @Test
    void failsClearlyWhenConsistentHashDependencyIsMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("com.google.common.hash"))
                .withPropertyValues("framework.rabbitmq.partitioned.selector.algorithm=consistent-hash")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Rabbit partitioned consistent-hash selector requires Guava "
                                            + "on the application classpath."
                            );
                });
    }

    @Test
    void backsOffToCustomSelectorWithoutGuava() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("com.google.common.hash"))
                .withUserConfiguration(CustomSelectorConfiguration.class)
                .withPropertyValues("framework.rabbitmq.partitioned.selector.algorithm=consistent-hash")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MessageQueueSelector.class))
                            .isSameAs(context.getBean("customMessageQueueSelector"));
                });
    }

    @Test
    void doesNotConfigureSelectorForConsistentHashPluginRouting() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.partitioned.routing-mode=consistent-hash-plugin")
                .run(context -> assertThat(context).doesNotHaveBean(MessageQueueSelector.class));
    }

    @Test
    void doesNotConfigureTemplateWithoutPublisherConfirm() {
        this.contextRunner
                .withUserConfiguration(RabbitTemplateConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(PartitionedRabbitTemplate.class));
    }

    @Test
    void configuresTemplateWhenRequiredBeansExist() {
        this.contextRunner
                .withUserConfiguration(RabbitTemplateConfiguration.class, PublisherConfirmConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(PartitionedRabbitTemplate.class));
    }

    @Test
    void configuresInitializerForCustomDeclarerWithoutAmqpAdmin() {
        this.contextRunner
                .withUserConfiguration(CustomTopologyDeclarerConfiguration.class)
                .run(context -> assertThat(context)
                        .hasBean("partitionedRabbitTopologyInitializer")
                        .hasSingleBean(RabbitTopologyInitializer.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSelectorConfiguration {

        @Bean
        MessageQueueSelector customMessageQueueSelector() {
            return (partitions, partitionArg) -> 0;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RabbitTemplateConfiguration {

        @Bean
        RabbitTemplate rabbitTemplate() {
            return new TestRabbitTemplate();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class PublisherConfirmConfiguration {

        @Bean
        RabbitPublisherConfirm rabbitPublisherConfirm() {
            return new RabbitPublisherConfirm(new RabbitMessageProperties());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTopologyDeclarerConfiguration {

        @Bean
        PartitionedRabbitTopologyDeclarer customPartitionedRabbitTopologyDeclarer() {
            return destination -> {
            };
        }
    }

    static class TestRabbitTemplate extends RabbitTemplate {

        @Override
        public void afterPropertiesSet() {
        }
    }
}
