package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public class IntegrationEventOutboxPublisher {
    private final IntegrationEventOutboxPersistenceRepository repository;
    private final IntegrationEventOutboxPersistenceMapper mapper;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Transactional
    public boolean publish(@NotBlank String batchId) {
        List<IntegrationEventOutboxDO> outboxEvents = repository.findByBatchId(batchId);
        if (outboxEvents.isEmpty() || outboxEvents.stream().anyMatch(this::isNotPending)) {
            return false;
        }

        Integer claimedCount = repository.markPublishingByBatchId(batchId, clock.instant());
        if (!Integer.valueOf(outboxEvents.size()).equals(claimedCount)) {
            return false;
        }

        List<IntegrationEvent> events = outboxEvents.stream()
                .map(mapper::toIntegrationEvent)
                .toList();
        integrationEventPublisher.publishAll(events);

        Integer publishedCount = repository.markPublishedByBatchId(batchId, clock.instant());
        if (!Integer.valueOf(outboxEvents.size()).equals(publishedCount)) {
            throw new IllegalStateException("Failed to mark integration event batch as published: " + batchId);
        }
        return true;
    }

    private boolean isNotPending(IntegrationEventOutboxDO outbox) {
        return outbox.getStatus() != OutboxStatus.PENDING;
    }
}
