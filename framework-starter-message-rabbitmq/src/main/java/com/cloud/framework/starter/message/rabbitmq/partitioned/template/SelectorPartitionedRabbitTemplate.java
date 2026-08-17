package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import com.cloud.framework.message.support.MessageQueueSelector;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitRoute;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class SelectorPartitionedRabbitTemplate extends AbstractPartitionedRabbitTemplate {

    private final MessageQueueSelector queueSelector;

    public SelectorPartitionedRabbitTemplate(
            RabbitTemplate rabbitTemplate,
            RabbitPublisherConfirm publisherConfirm,
            PartitionedRabbitTopologyRegistry topologyRegistry,
            MessageQueueSelector queueSelector
            ) {
        super(rabbitTemplate, publisherConfirm, topologyRegistry);
        this.queueSelector = queueSelector;
    }

    @Override
    protected RabbitRoute route(String destination, Object partitionArg) {
        Integer index = this.queueSelector.select(this.topologyRegistry.partitions(destination), partitionArg);
        return new RabbitRoute(
                PartitionedRabbitMessageSupport.exchange(destination),
                PartitionedRabbitMessageSupport.routingKey(index)
        );
    }
}
