package com.cloud.framework.starter.message.rabbitmq.partitioned.topology;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

public class SelectorPartitionedRabbitTopologyDeclarer extends AbstractPartitionedRabbitTopologyDeclarer<DirectExchange> {

    public SelectorPartitionedRabbitTopologyDeclarer(AmqpAdmin amqpAdmin, PartitionedRabbitTopologyRegistry topologyRegistry) {
        super(amqpAdmin, topologyRegistry);
    }

    @Override
    protected DirectExchange exchange(String destination) {
        return new DirectExchange(PartitionedRabbitMessageSupport.exchange(destination), true, false);
    }

    @Override
    protected Binding binding(String destination, Integer index, DirectExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(PartitionedRabbitMessageSupport.indexBindingKey(index));
    }
}
