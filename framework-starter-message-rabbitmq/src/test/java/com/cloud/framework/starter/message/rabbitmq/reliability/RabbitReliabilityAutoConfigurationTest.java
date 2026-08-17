package com.cloud.framework.starter.message.rabbitmq.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RabbitReliabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitReliabilityAutoConfiguration.class))
            .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class));

    @Test
    void preservesNativeRabbitConfigurationWhenReliabilityModeIsMissing() {
        this.contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean("rabbitProducerReliabilityBeanPostProcessor");
            assertThat(context).doesNotHaveBean("rabbitReliabilityTemplateCustomizer");
            assertThat(context).doesNotHaveBean("rabbitConsumerTransactionBeanPostProcessor");
            assertThat(context).doesNotHaveBean(RabbitTransactionManager.class);
        });
    }

    @Test
    void groupsProducerTransactionInfrastructure() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.producer.reliability-mode=transaction")
                .run(context -> {
                    assertThat(context).hasBean("rabbitProducerReliabilityBeanPostProcessor");
                    assertThat(context).hasBean("rabbitReliabilityTemplateCustomizer");
                    assertThat(context).hasSingleBean(RabbitTransactionManager.class);
                    assertThat(context).doesNotHaveBean("rabbitConsumerTransactionBeanPostProcessor");
                });
    }

    @Test
    void groupsPublisherConfirmInfrastructure() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.producer.reliability-mode=publisher-confirm")
                .run(context -> {
                    assertThat(context).hasBean("rabbitProducerReliabilityBeanPostProcessor");
                    assertThat(context).hasBean("rabbitReliabilityTemplateCustomizer");
                    assertThat(context).doesNotHaveBean(RabbitTransactionManager.class);
                    assertThat(context).doesNotHaveBean("rabbitConsumerTransactionBeanPostProcessor");
                });
    }

    @Test
    void groupsConsumerTransactionInfrastructureIndependently() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.consumer.reliability-mode=transaction")
                .run(context -> {
                    assertThat(context).hasBean("rabbitConsumerTransactionBeanPostProcessor");
                    assertThat(context).doesNotHaveBean("rabbitProducerReliabilityBeanPostProcessor");
                    assertThat(context).doesNotHaveBean("rabbitReliabilityTemplateCustomizer");
                });
    }
}
