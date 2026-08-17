package com.cloud.framework.starter.message.rabbitmq.topology;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitTopologyInitializerTest {

    @Test
    void shouldSkipTopologyWithoutDestinations() {
        RabbitTopologyRegistry registry = mock(RabbitTopologyRegistry.class);
        RabbitTopologyDeclarer declarer = mock(RabbitTopologyDeclarer.class);
        when(registry.destinations()).thenReturn(Set.of());

        new RabbitTopologyInitializer(registry, declarer).initialize();

        verify(declarer, never()).initialize();
    }

    @Test
    void shouldInitializeConfiguredTopologyOnlyOnce() {
        RabbitTopologyRegistry registry = mock(RabbitTopologyRegistry.class);
        RabbitTopologyDeclarer declarer = mock(RabbitTopologyDeclarer.class);
        when(registry.destinations()).thenReturn(Set.of("orders"));
        RabbitTopologyInitializer initializer = new RabbitTopologyInitializer(registry, declarer);

        initializer.initialize();
        initializer.initialize();

        verify(declarer, times(1)).initialize();
        verify(declarer, times(1)).declare("orders");
    }
}
