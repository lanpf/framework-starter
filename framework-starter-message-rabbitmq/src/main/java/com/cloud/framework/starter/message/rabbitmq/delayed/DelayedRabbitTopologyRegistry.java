package com.cloud.framework.starter.message.rabbitmq.delayed;

import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyRegistry;
import java.time.Duration;

public interface DelayedRabbitTopologyRegistry extends RabbitTopologyRegistry {

    Integer levels();

    Duration tickDuration();

    boolean quorum();

    boolean atLeastOnce();

    boolean deadLetterEnabled();
}
