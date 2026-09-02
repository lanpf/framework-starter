package com.cloud.framework.starter.outbox.reliable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ReliableIntegrationEventOutboxTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void assignsOneBatchAndPublishesABatchSignal() {
        IntegrationEventOutboxEnvelopePersistenceRepository repository = mock(IntegrationEventOutboxEnvelopePersistenceRepository.class);
        IntegrationEventOutboxEnvelopePersistenceMapper mapper = mock(IntegrationEventOutboxEnvelopePersistenceMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        IntegrationEvent firstEvent = event("event-1");
        IntegrationEvent secondEvent = event("event-2");
        IntegrationEventOutboxEnvelope firstEnvelope = envelope("event-1", "event-1", 0);
        IntegrationEventOutboxEnvelope secondEnvelope = envelope("event-2", "event-1", 1);
        when(mapper.toEnvelope(firstEvent, NOW, "event-1", 0)).thenReturn(firstEnvelope);
        when(mapper.toEnvelope(secondEvent, NOW, "event-1", 1)).thenReturn(secondEnvelope);
        ReliableIntegrationEventOutbox outbox =
                new ReliableIntegrationEventOutbox(repository, mapper, eventPublisher, CLOCK);

        outbox.appendAll(List.of(firstEvent, secondEvent));

        verify(repository).saveAll(List.of(firstEnvelope, secondEnvelope));
        ArgumentCaptor<IntegrationEventOutboxSignal> signal = ArgumentCaptor.forClass(IntegrationEventOutboxSignal.class);
        verify(eventPublisher).publishEvent(signal.capture());
        assertThat(signal.getValue().batchId()).isEqualTo("event-1");
    }

    private IntegrationEvent event(String eventId) {
        IntegrationEvent event = mock(IntegrationEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getAggregateType()).thenReturn("TestAggregate");
        when(event.getAggregateId()).thenReturn("aggregate-1");
        return event;
    }

    private IntegrationEventOutboxEnvelope envelope(String eventId, String batchId, int batchSequence) {
        return new IntegrationEventOutboxEnvelope(
                eventId,
                "TestEvent",
                null,
                batchId,
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
