package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence;

import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class IntegrationEventOutboxJpaPersistenceRepository implements IntegrationEventOutboxPersistenceRepository {
    private final IntegrationEventOutboxJpaRepository repository;

    @Override
    public void saveAll(List<IntegrationEventOutboxDO> events) {
        this.repository.saveAll(events);
    }

    @Override
    public List<IntegrationEventOutboxDO> findByBatchId(String batchId) {
        return this.repository.findByBatchIdOrderByBatchSequenceAsc(batchId);
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
