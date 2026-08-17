package com.cloud.framework.starter.outbox.reliable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
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
        IntegrationEventOutboxPersistenceRepository repository = mock(IntegrationEventOutboxPersistenceRepository.class);
        IntegrationEventOutboxPersistenceMapper mapper = mock(IntegrationEventOutboxPersistenceMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        IntegrationEvent firstEvent = event("event-1");
        IntegrationEvent secondEvent = event("event-2");
        IntegrationEventOutboxDO firstOutbox = new IntegrationEventOutboxDO();
        firstOutbox.setEventId("event-1");
        IntegrationEventOutboxDO secondOutbox = new IntegrationEventOutboxDO();
        secondOutbox.setEventId("event-2");
        when(mapper.toDataObject(firstEvent, NOW)).thenReturn(firstOutbox);
        when(mapper.toDataObject(secondEvent, NOW)).thenReturn(secondOutbox);
        ReliableIntegrationEventOutbox outbox =
                new ReliableIntegrationEventOutbox(repository, mapper, eventPublisher, CLOCK);

        outbox.appendAll(List.of(firstEvent, secondEvent));

        assertThat(firstOutbox.getBatchId()).isEqualTo("event-1");
        assertThat(firstOutbox.getBatchSequence()).isZero();
        assertThat(secondOutbox.getBatchId()).isEqualTo("event-1");
        assertThat(secondOutbox.getBatchSequence()).isEqualTo(1);
        ArgumentCaptor<IntegrationEventOutboxSignal> signal = ArgumentCaptor.forClass(IntegrationEventOutboxSignal.class);
        verify(eventPublisher).publishEvent(signal.capture());
        assertThat(signal.getValue().getBatchId()).isEqualTo("event-1");
        verify(repository).saveAll(any());
    }

    private IntegrationEvent event(String eventId) {
        IntegrationEvent event = mock(IntegrationEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getAggregateType()).thenReturn("TestAggregate");
        when(event.getAggregateId()).thenReturn("aggregate-1");
        return event;
    }
}
