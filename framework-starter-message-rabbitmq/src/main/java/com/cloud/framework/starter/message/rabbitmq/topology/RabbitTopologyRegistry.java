package com.cloud.framework.starter.message.rabbitmq.topology;

import java.util.Set;

public interface RabbitTopologyRegistry {

    String DESTINATION_PATTERN = "^[^*#]+$";

    Set<String> destinations();
}
