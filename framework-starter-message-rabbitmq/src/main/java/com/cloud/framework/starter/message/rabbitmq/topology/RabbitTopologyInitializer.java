package com.cloud.framework.starter.message.rabbitmq.topology;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class RabbitTopologyInitializer implements SmartInitializingSingleton {

    private final RabbitTopologyRegistry registry;

    private final RabbitTopologyDeclarer declarer;

    private final AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public void afterSingletonsInstantiated() {
        initialize();
    }

    public void initialize() {
        if (!CollectionUtils.isEmpty(this.registry.destinations()) && this.initialized.compareAndSet(false, true)) {
            this.declarer.initialize();
            this.registry.destinations().forEach(this.declarer::declare);
        }
    }
}
