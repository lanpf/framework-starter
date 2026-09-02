package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence;

import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class IntegrationEventOutboxJpaPersistenceRepository implements IntegrationEventOutboxEnvelopePersistenceRepository {
    private final IntegrationEventOutboxJpaRepository repository;
    private final IntegrationEventOutboxJpaPersistenceMapper mapper;

    @Override
    public void saveAll(List<IntegrationEventOutboxEnvelope> events) {
        this.repository.saveAll(events.stream().map(this.mapper::toDataObject).toList());
    }

    @Override
    public List<IntegrationEventOutboxEnvelope> findByBatchId(String batchId) {
        return this.repository.findByBatchIdOrderByBatchSequenceAsc(batchId).stream()
                .map(this.mapper::toEnvelope)
                .toList();
    }

    @Override
    public List<String> findBatchIdsByStatusAndRetryCountLessThan(
            OutboxStatus status,
            Integer retryCount,
            Integer limit
    ) {
        return this.repository.findBatchIdsByStatusAndRetryCountLessThan(
                status,
                retryCount,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<String> findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
            OutboxStatus status,
            Instant updatedBefore,
            Integer retryCount,
            Integer limit
    ) {
        return this.repository.findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
                status,
                updatedBefore,
                retryCount,
                PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional
    public Integer markPublishingByBatchId(String batchId, Instant publishingAt) {
        return this.repository.markPublishingByBatchId(
                batchId,
                publishingAt,
                OutboxStatus.PENDING,
                OutboxStatus.PUBLISHING
        );
    }

    @Override
    @Transactional
    public Integer markPublishedByBatchId(String batchId, Instant publishedAt) {
        return this.repository.markPublishedByBatchId(
                batchId,
                publishedAt,
                OutboxStatus.PUBLISHING,
                OutboxStatus.PUBLISHED
        );
    }

    @Override
    @Transactional
    public Integer restorePendingByBatchId(String batchId, Instant publishingAt, Instant pendingAt) {
        return this.repository.restorePendingByBatchId(
                batchId,
                publishingAt,
                pendingAt,
                OutboxStatus.PUBLISHING,
                OutboxStatus.PENDING
        );
    }

    @Override
    @Transactional
    public Integer restoreExpiredPublishingByBatchId(String batchId, Instant publishingBefore, Instant pendingAt) {
        return this.repository.restoreExpiredPublishingByBatchId(
                batchId,
                publishingBefore,
                pendingAt,
                OutboxStatus.PUBLISHING,
                OutboxStatus.PENDING
        );
    }

    @Override
    @Transactional
    public void markPublishFailedByBatchId(
            String batchId,
            Instant failedAt,
            String lastError,
            Integer maxRetryCount
    ) {
        this.repository.findByBatchIdOrderByBatchSequenceAsc(batchId).stream()
                .filter(outbox -> outbox.getStatus() == OutboxStatus.PENDING)
                .filter(outbox -> outbox.getRetryCount() < maxRetryCount)
                .forEach(outbox -> markPublishFailed(outbox, failedAt, lastError, maxRetryCount));
    }

    private void markPublishFailed(
            IntegrationEventOutboxDO outbox,
            Instant failedAt,
            String lastError,
            Integer maxRetryCount
    ) {
        Integer nextRetryCount = outbox.getRetryCount() + 1;
        outbox.setRetryCount(nextRetryCount);
        outbox.setLastError(lastError);
        outbox.setUpdatedAt(failedAt);
        if (nextRetryCount >= maxRetryCount) {
            outbox.setStatus(OutboxStatus.FAILED);
            outbox.setFailedAt(failedAt);
        }
        this.repository.save(outbox);
    }
}
