package com.cloud.framework.starter.outbox.reliable;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.framework.starter.outbox.IntegrationEventOutboxProperties;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class IntegrationEventOutboxFallbackPublisherTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration PUBLISHING_TIMEOUT = Duration.ofMinutes(5);

    @Test
    void shouldRecoverExpiredPublishingBatchesBeforePublishingPendingBatches() {
        IntegrationEventOutboxEnvelopePersistenceRepository repository = mock(IntegrationEventOutboxEnvelopePersistenceRepository.class);
        IntegrationEventOutboxPublisher outboxPublisher = mock(IntegrationEventOutboxPublisher.class);
        IntegrationEventOutboxProperties properties = new IntegrationEventOutboxProperties();
        properties.getRecovery().setPublishingTimeout(PUBLISHING_TIMEOUT.toMillis());
        Instant publishingBefore = NOW.minus(PUBLISHING_TIMEOUT);
        when(repository.findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
                OutboxStatus.PUBLISHING, publishingBefore, 3, 10)).thenReturn(List.of("expired-batch"));
        when(repository.restoreExpiredPublishingByBatchId("expired-batch", publishingBefore, NOW)).thenReturn(2);
        when(repository.findBatchIdsByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 3, 10))
                .thenReturn(List.of("expired-batch"));
        when(outboxPublisher.publish("expired-batch")).thenReturn(true);
        IntegrationEventOutboxFallbackPublisher publisher = new IntegrationEventOutboxFallbackPublisher(
                repository, outboxPublisher, CLOCK, properties);

        publisher.publish(10, 3);

        InOrder invocationOrder = inOrder(repository, outboxPublisher);
        invocationOrder.verify(repository).findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
                OutboxStatus.PUBLISHING, publishingBefore, 3, 10);
        invocationOrder.verify(repository)
                .restoreExpiredPublishingByBatchId("expired-batch", publishingBefore, NOW);
        invocationOrder.verify(repository)
                .findBatchIdsByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 3, 10);
        invocationOrder.verify(outboxPublisher).publish("expired-batch");
    }
}
