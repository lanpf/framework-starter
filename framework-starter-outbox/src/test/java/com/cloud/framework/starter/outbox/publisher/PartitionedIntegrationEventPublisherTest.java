package com.cloud.framework.starter.outbox.publisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.framework.message.PartitionedOperations;
import com.cloud.framework.message.integration.IntegrationEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionedIntegrationEventPublisherTest {
    @Test
    void publishesEventsWithTheSameAggregateTypeAsOneBatch() {
        PartitionedOperations partitionedOperations = mock(PartitionedOperations.class);
        PartitionedIntegrationEventPublisher publisher =
                new PartitionedIntegrationEventPublisher(partitionedOperations);
        IntegrationEvent first = event("person", "person-1");
        IntegrationEvent second = event("person", "person-2");
        List<IntegrationEvent> events = List.of(first, second);

        publisher.publishAll(events);

        verify(partitionedOperations).convertAndSendBatch(
                eq("person"),
                eq(events),
                any(),
                any()
        );
    }

    @Test
    void rejectsEventsWithDifferentAggregateTypesBeforeSending() {
        PartitionedOperations partitionedOperations = mock(PartitionedOperations.class);
        PartitionedIntegrationEventPublisher publisher =
                new PartitionedIntegrationEventPublisher(partitionedOperations);

        assertThatThrownBy(() -> publisher.publishAll(List.of(
                event("person", "person-1"),
                event("user", "user-1")
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one aggregate type");
        verifyNoInteractions(partitionedOperations);
    }

    private IntegrationEvent event(String aggregateType, String aggregateId) {
        IntegrationEvent event = mock(IntegrationEvent.class);
        when(event.getAggregateType()).thenReturn(aggregateType);
        when(event.getAggregateId()).thenReturn(aggregateId);
        return event;
    }
}
