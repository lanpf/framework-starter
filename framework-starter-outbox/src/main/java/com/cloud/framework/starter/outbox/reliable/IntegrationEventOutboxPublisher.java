package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import com.cloud.framework.starter.outbox.persistence.mapstruct.IntegrationEventOutboxPayloadConverter;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public class IntegrationEventOutboxPublisher {
    private final IntegrationEventOutboxEnvelopePersistenceRepository repository;
    private final IntegrationEventOutboxPayloadConverter converter;
    private final IntegrationEventPublisher eventPublisher;
    private final Clock clock;

    public boolean publish(@NotBlank String batchId) {
        List<IntegrationEventOutboxEnvelope> outboxEvents = repository.findByBatchId(batchId);
        if (CollectionUtils.isEmpty(outboxEvents) || outboxEvents.stream().anyMatch(outbox -> outbox.status() != OutboxStatus.PENDING)) {
            return false;
        }

        var publishingAt = clock.instant();
        Integer claimedCount = repository.markPublishingByBatchId(batchId, publishingAt);
        if (!ObjectUtils.nullSafeEquals(claimedCount, outboxEvents.size())) {
            return false;
        }

        try {
            List<IntegrationEvent> events = outboxEvents.stream()
                    .map(converter::deserialize)
                    .toList();
            eventPublisher.publishAll(events);

            Integer publishedCount = repository.markPublishedByBatchId(batchId, clock.instant());
            if (!ObjectUtils.nullSafeEquals(publishedCount, outboxEvents.size())) {
                throw new IllegalStateException("Failed to mark integration event batch as published: " + batchId);
            }
        } catch (RuntimeException ex) {
            repository.restorePendingByBatchId(batchId, publishingAt, clock.instant());
            throw ex;
        }
        return true;
    }
}
