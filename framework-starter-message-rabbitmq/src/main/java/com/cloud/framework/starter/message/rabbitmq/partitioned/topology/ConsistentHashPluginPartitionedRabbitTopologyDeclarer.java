package com.cloud.framework.starter.message.rabbitmq.partitioned.topology;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.util.HashMap;

public class ConsistentHashPluginPartitionedRabbitTopologyDeclarer extends AbstractPartitionedRabbitTopologyDeclarer<CustomExchange> {

    public ConsistentHashPluginPartitionedRabbitTopologyDeclarer(AmqpAdmin amqpAdmin, PartitionedRabbitTopologyRegistry topologyRegistry) {
        super(amqpAdmin, topologyRegistry);
    }

    @Override
    protected CustomExchange exchange(String destination) {
        return new CustomExchange(
                PartitionedRabbitMessageSupport.exchange(destination),
                "x-consistent-hash",
                true,
                false,
                new HashMap<>()
        );
    }

    @Override
    protected Binding binding(String destination, Integer index, CustomExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(PartitionedRabbitMessageSupport.weightBindingKey(this.topologyRegistry.weight(destination)))
                .noargs();
    }
}
