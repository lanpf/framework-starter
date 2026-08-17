package com.cloud.framework.starter.message.rabbitmq.delayed;

import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import com.cloud.framework.starter.message.rabbitmq.delayed.template.DelayedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.delayed.topology.DelayedRabbitTopologyDeclarer;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitAutoConfiguration;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DelayedRabbitAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DelayedRabbitAutoConfiguration.class))
            .withBean(RabbitTemplate.class, TestRabbitTemplate::new)
            .withBean(AmqpAdmin.class, TestAmqpAdmin::new)
            .withBean(RabbitPublisherConfirm.class, () -> new RabbitPublisherConfirm(new RabbitMessageProperties()));

    @Test
    void shouldConfigureDelayedRabbitCapability() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DelayedRabbitProperties.class);
            assertThat(context).hasSingleBean(DelayedRabbitTopologyDeclarer.class);
            assertThat(context).hasSingleBean(RabbitTopologyInitializer.class);
            assertThat(context).hasSingleBean(DelayedRabbitTemplate.class);
        });
    }

    @Test
    void shouldConfigureIndependentTopologyInitializers() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DelayedRabbitAutoConfiguration.class,
                        PartitionedRabbitAutoConfiguration.class
                ))
                .withBean(RabbitTemplate.class, TestRabbitTemplate::new)
                .withBean(AmqpAdmin.class, TestAmqpAdmin::new)
                .withBean(
                        RabbitPublisherConfirm.class,
                        () -> new RabbitPublisherConfirm(new RabbitMessageProperties())
                )
                .run(context -> {
                    assertThat(context).hasBean("delayedRabbitTopologyInitializer");
                    assertThat(context).hasBean("partitionedRabbitTopologyInitializer");
                    assertThat(context).getBeans(RabbitTopologyInitializer.class).hasSize(2);
                });
    }

    static class TestRabbitTemplate extends RabbitTemplate {

        @Override
        public void afterPropertiesSet() {
        }
    }

    static class TestAmqpAdmin implements AmqpAdmin {

        @Override
        public void declareExchange(Exchange exchange) {
        }

        @Override
        public boolean deleteExchange(String exchangeName) {
            return false;
        }

        @Override
        public Queue declareQueue() {
            return null;
        }

        @Override
        public String declareQueue(Queue queue) {
            return null;
        }

        @Override
        public boolean deleteQueue(String queueName) {
            return false;
        }

        @Override
        public void deleteQueue(String queueName, boolean unused, boolean empty) {
        }

        @Override
        public void purgeQueue(String queueName, boolean noWait) {
        }

        @Override
        public int purgeQueue(String queueName) {
            return 0;
        }

        @Override
        public void declareBinding(Binding binding) {
        }

        @Override
        public void removeBinding(Binding binding) {
        }

        @Override
        public Properties getQueueProperties(String queueName) {
            return null;
        }

        @Override
        public QueueInformation getQueueInfo(String queueName) {
            return null;
        }
    }
}
