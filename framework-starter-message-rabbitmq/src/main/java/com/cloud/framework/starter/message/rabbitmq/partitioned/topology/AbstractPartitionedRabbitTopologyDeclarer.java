package com.cloud.framework.starter.message.rabbitmq.partitioned.topology;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public abstract class AbstractPartitionedRabbitTopologyDeclarer<E extends Exchange> implements PartitionedRabbitTopologyDeclarer {

    protected final AmqpAdmin amqpAdmin;

    protected final PartitionedRabbitTopologyRegistry topologyRegistry;

    @Override
    public void declare(String destination) {
        E exchange = exchange(destination);
        this.amqpAdmin.declareExchange(exchange);
        DirectExchange deadLetterExchange = declareDeadLetterExchange(destination);
        for (int index = 0; index < this.topologyRegistry.partitions(destination); index++) {
            Queue queue = queue(destination, index);
            this.amqpAdmin.declareQueue(queue);
            Binding binding = binding(destination, index, exchange, queue);
            this.amqpAdmin.declareBinding(binding);
            declareDeadLetterTopology(destination, index, deadLetterExchange);
        }
    }

    protected abstract E exchange(String destination);

    protected Queue queue(String destination, Integer index) {
        QueueBuilder builder = QueueBuilder.durable(PartitionedRabbitMessageSupport.queue(destination, index))
                .singleActiveConsumer();
        if (this.topologyRegistry.deadLetterEnabled()) {
            builder.deadLetterExchange(PartitionedRabbitMessageSupport.deadLetterExchange(destination))
                    .deadLetterRoutingKey(PartitionedRabbitMessageSupport.routingKey(index));
        }
        return builder.build();
    }

    protected abstract Binding binding(String destination, Integer index, E exchange, Queue queue);


    private DirectExchange declareDeadLetterExchange(String destination) {
        if (!this.topologyRegistry.deadLetterEnabled()) {
            return null;
        }
        DirectExchange exchange = new DirectExchange(
                PartitionedRabbitMessageSupport.deadLetterExchange(destination), true, false);
        this.amqpAdmin.declareExchange(exchange);
        return exchange;
    }

    private void declareDeadLetterTopology(String destination, Integer index, DirectExchange exchange) {
        if (!this.topologyRegistry.deadLetterEnabled()) {
            return;
        }
        Queue queue = QueueBuilder.durable(PartitionedRabbitMessageSupport.deadLetterQueue(destination, index)).build();
        this.amqpAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with(PartitionedRabbitMessageSupport.indexBindingKey(index));
        this.amqpAdmin.declareBinding(binding);
    }
}
