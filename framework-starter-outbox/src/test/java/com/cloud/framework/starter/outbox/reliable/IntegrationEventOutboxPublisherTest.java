package com.cloud.framework.starter.outbox.reliable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import com.cloud.framework.starter.outbox.persistence.mapstruct.IntegrationEventOutboxPayloadConverter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationEventOutboxPublisherTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldPublishAndMarkWholeBatch() {
        IntegrationEventOutboxEnvelopePersistenceRepository repository = mock(IntegrationEventOutboxEnvelopePersistenceRepository.class);
        IntegrationEventOutboxPayloadConverter converter = mock(IntegrationEventOutboxPayloadConverter.class);
        IntegrationEventPublisher eventPublisher = mock(IntegrationEventPublisher.class);
        IntegrationEventOutboxEnvelope firstEnvelope = envelope("event-1", 0);
        IntegrationEventOutboxEnvelope secondEnvelope = envelope("event-2", 1);
        IntegrationEvent firstEvent = mock(IntegrationEvent.class);
        IntegrationEvent secondEvent = mock(IntegrationEvent.class);
        when(repository.findByBatchId("batch-1")).thenReturn(List.of(firstEnvelope, secondEnvelope));
        when(repository.markPublishingByBatchId("batch-1", NOW))
                .thenReturn(2);
        when(repository.markPublishedByBatchId("batch-1", NOW))
                .thenReturn(2);
        when(converter.deserialize(firstEnvelope)).thenReturn(firstEvent);
        when(converter.deserialize(secondEnvelope)).thenReturn(secondEvent);
        IntegrationEventOutboxPublisher publisher =
                new IntegrationEventOutboxPublisher(repository, converter, eventPublisher, CLOCK);

        assertThat(publisher.publish("batch-1")).isTrue();

        verify(eventPublisher).publishAll(List.of(firstEvent, secondEvent));
        verify(repository).markPublishedByBatchId(
                "batch-1",
                NOW
        );
    }

    @Test
    void shouldRestorePendingBatchWhenMessagePublicationFails() {
        IntegrationEventOutboxEnvelopePersistenceRepository repository = mock(IntegrationEventOutboxEnvelopePersistenceRepository.class);
        IntegrationEventOutboxPayloadConverter converter = mock(IntegrationEventOutboxPayloadConverter.class);
        IntegrationEventPublisher eventPublisher = mock(IntegrationEventPublisher.class);
        IntegrationEventOutboxEnvelope outboxEvent = envelope("event-1", 0);
        IntegrationEvent event = mock(IntegrationEvent.class);
        when(repository.findByBatchId("batch-1")).thenReturn(List.of(outboxEvent));
        when(repository.markPublishingByBatchId("batch-1", NOW)).thenReturn(1);
        when(converter.deserialize(outboxEvent)).thenReturn(event);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(eventPublisher).publishAll(List.of(event));
        IntegrationEventOutboxPublisher publisher =
                new IntegrationEventOutboxPublisher(repository, converter, eventPublisher, CLOCK);

        assertThatThrownBy(() -> publisher.publish("batch-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        verify(repository).restorePendingByBatchId("batch-1", NOW, NOW);
    }

    private IntegrationEventOutboxEnvelope envelope(String eventId, Integer batchSequence) {
        return new IntegrationEventOutboxEnvelope(
                eventId,
                "TestEvent",
                null,
                "batch-1",
                batchSequence,
                "TestAggregate",
                "aggregate-1",
                NOW,
                "payload",
                OutboxStatus.PENDING,
                0,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }
}
