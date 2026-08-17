package com.cloud.framework.starter.outbox.reliable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationEventOutboxPublisherTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void publishesAndMarksTheWholeBatch() {
        IntegrationEventOutboxPersistenceRepository repository = mock(IntegrationEventOutboxPersistenceRepository.class);
        IntegrationEventOutboxPersistenceMapper mapper = mock(IntegrationEventOutboxPersistenceMapper.class);
        IntegrationEventPublisher eventPublisher = mock(IntegrationEventPublisher.class);
        IntegrationEventOutboxDO firstOutbox = outbox("event-1", 0);
        IntegrationEventOutboxDO secondOutbox = outbox("event-2", 1);
        IntegrationEvent firstEvent = mock(IntegrationEvent.class);
        IntegrationEvent secondEvent = mock(IntegrationEvent.class);
        when(repository.findByBatchId("batch-1")).thenReturn(List.of(firstOutbox, secondOutbox));
        when(repository.markPublishingByBatchId("batch-1", NOW))
                .thenReturn(2);
        when(repository.markPublishedByBatchId("batch-1", NOW))
                .thenReturn(2);
        when(mapper.toIntegrationEvent(firstOutbox)).thenReturn(firstEvent);
        when(mapper.toIntegrationEvent(secondOutbox)).thenReturn(secondEvent);
        IntegrationEventOutboxPublisher publisher =
                new IntegrationEventOutboxPublisher(repository, mapper, eventPublisher, CLOCK);

        assertThat(publisher.publish("batch-1")).isTrue();

        verify(eventPublisher).publishAll(List.of(firstEvent, secondEvent));
        verify(repository).markPublishedByBatchId(
                "batch-1",
                NOW
        );
    }

    private IntegrationEventOutboxDO outbox(String eventId, Integer batchSequence) {
        IntegrationEventOutboxDO outbox = new IntegrationEventOutboxDO();
        outbox.setEventId(eventId);
        outbox.setBatchId("batch-1");
        outbox.setBatchSequence(batchSequence);
        outbox.setStatus(OutboxStatus.PENDING);
        return outbox;
    }
}
