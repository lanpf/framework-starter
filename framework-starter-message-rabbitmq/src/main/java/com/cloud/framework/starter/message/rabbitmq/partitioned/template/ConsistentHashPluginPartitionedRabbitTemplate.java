package com.cloud.framework.starter.message.rabbitmq.partitioned.template;

import com.cloud.framework.message.support.MessageHeaders;
import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.reliability.RabbitPublisherConfirm;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitRoute;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class ConsistentHashPluginPartitionedRabbitTemplate extends AbstractPartitionedRabbitTemplate {

    public ConsistentHashPluginPartitionedRabbitTemplate(
            RabbitTemplate rabbitTemplate,
            RabbitPublisherConfirm publisherConfirm,
            PartitionedRabbitTopologyRegistry topologyRegistry) {
        super(rabbitTemplate, publisherConfirm, topologyRegistry);
    }

    @Override
    protected RabbitRoute route(String destination, Object partitionArg) {
        return new RabbitRoute(
                PartitionedRabbitMessageSupport.exchange(destination),
                MessageHeaders.resolvePartitionKey(partitionArg).toString()
        );
    }
}
