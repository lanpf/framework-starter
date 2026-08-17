package com.cloud.framework.starter.message.rabbitmq.delayed.topology;

import com.cloud.framework.starter.message.rabbitmq.delayed.DelayedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.delayed.support.DelayedRabbitMessageSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public class BinaryDelayedRabbitTopologyDeclarer implements DelayedRabbitTopologyDeclarer {

    private static final String AT_LEAST_ONCE = "at-least-once";

    private final AmqpAdmin amqpAdmin;

    private final DelayedRabbitTopologyRegistry topologyRegistry;

    @Override
    public void initialize() {
        TopicExchange deliveryExchange = new TopicExchange(
                DelayedRabbitMessageSupport.deliveryExchange(),
                true,
                false
        );
        this.amqpAdmin.declareExchange(deliveryExchange);
        if (this.topologyRegistry.deadLetterEnabled()) {
            this.amqpAdmin.declareExchange(deliveryDeadLetterExchange());
        }

        for (int level = this.topologyRegistry.levels() - 1; level >= 0; level--) {
            String levelExchangeName = DelayedRabbitMessageSupport.levelExchange(level);
            TopicExchange levelExchange = new TopicExchange(levelExchangeName, true, false);
            this.amqpAdmin.declareExchange(levelExchange);
        }

        for (int level = this.topologyRegistry.levels() - 1; level >= 0; level--) {
            String levelExchangeName = DelayedRabbitMessageSupport.levelExchange(level);
            TopicExchange levelExchange = new TopicExchange(levelExchangeName, true, false);
            String nextExchangeName = level == 0
                    ? DelayedRabbitMessageSupport.deliveryExchange()
                    : DelayedRabbitMessageSupport.levelExchange(level - 1);
            long ttl = Math.multiplyExact(this.topologyRegistry.tickDuration().toMillis(), 1L << level);
            QueueBuilder levelQueueBuilder = queueBuilder(DelayedRabbitMessageSupport.levelQueue(level))
                    .withArgument("x-message-ttl", ttl)
                    .deadLetterExchange(nextExchangeName);
            configureAtLeastOnce(levelQueueBuilder);
            Queue levelQueue = levelQueueBuilder.build();
            this.amqpAdmin.declareQueue(levelQueue);

            Binding waitingLevelBinding = BindingBuilder.bind(levelQueue)
                    .to(levelExchange)
                    .with(DelayedRabbitMessageSupport.waitingLevelBindingKey(level, this.topologyRegistry.levels()));
            this.amqpAdmin.declareBinding(waitingLevelBinding);

            Binding bypassLevelBinding = new Binding(
                    nextExchangeName,
                    Binding.DestinationType.EXCHANGE,
                    levelExchangeName,
                    DelayedRabbitMessageSupport.bypassLevelBindingKey(level, this.topologyRegistry.levels()),
                    null
            );
            this.amqpAdmin.declareBinding(bypassLevelBinding);
        }
    }

    @Override
    public void declare(String destination) {
        QueueBuilder deliveryQueueBuilder = queueBuilder(DelayedRabbitMessageSupport.deliveryQueue(destination));
        if (this.topologyRegistry.deadLetterEnabled()) {
            deliveryQueueBuilder
                    .deadLetterExchange(DelayedRabbitMessageSupport.deliveryDeadLetterExchange())
                    .deadLetterRoutingKey(destination);
            configureAtLeastOnce(deliveryQueueBuilder);
        }
        Queue deliveryQueue = deliveryQueueBuilder.build();
        this.amqpAdmin.declareQueue(deliveryQueue);

        TopicExchange deliveryExchange = new TopicExchange(
                DelayedRabbitMessageSupport.deliveryExchange(),
                true,
                false
        );
        Binding immediateDeliveryBinding = BindingBuilder.bind(deliveryQueue)
                .to(deliveryExchange)
                .with(DelayedRabbitMessageSupport.immediateDeliveryBindingKey(destination));
        this.amqpAdmin.declareBinding(immediateDeliveryBinding);

        Binding delayedDeliveryBinding = BindingBuilder.bind(deliveryQueue)
                .to(deliveryExchange)
                .with(DelayedRabbitMessageSupport.delayedDeliveryBindingKey(
                        this.topologyRegistry.levels(),
                        destination
                ));
        this.amqpAdmin.declareBinding(delayedDeliveryBinding);

        declareDeliveryDeadLetterTopology(destination);
    }

    private void declareDeliveryDeadLetterTopology(String destination) {
        if (!this.topologyRegistry.deadLetterEnabled()) {
            return;
        }
        Queue queue = queueBuilder(DelayedRabbitMessageSupport.deliveryDeadLetterQueue(destination)).build();
        this.amqpAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(deliveryDeadLetterExchange()).with(destination);
        this.amqpAdmin.declareBinding(binding);
    }

    private DirectExchange deliveryDeadLetterExchange() {
        return new DirectExchange(
                DelayedRabbitMessageSupport.deliveryDeadLetterExchange(),
                true,
                false
        );
    }

    private QueueBuilder queueBuilder(String name) {
        QueueBuilder queueBuilder = QueueBuilder.durable(name);
        if (this.topologyRegistry.quorum()) {
            queueBuilder.quorum();
        }
        return queueBuilder;
    }

    private void configureAtLeastOnce(QueueBuilder queueBuilder) {
        if (this.topologyRegistry.atLeastOnce()) {
            queueBuilder.withArgument("x-dead-letter-strategy", AT_LEAST_ONCE)
                    .overflow(QueueBuilder.Overflow.rejectPublish);
        }
    }
}
