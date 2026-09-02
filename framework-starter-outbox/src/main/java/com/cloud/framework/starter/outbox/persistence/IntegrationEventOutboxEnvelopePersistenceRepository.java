package com.cloud.framework.starter.outbox.persistence;

import java.time.Instant;
import java.util.List;

public interface IntegrationEventOutboxEnvelopePersistenceRepository {
    void saveAll(List<IntegrationEventOutboxEnvelope> events);

    List<IntegrationEventOutboxEnvelope> findByBatchId(String batchId);

    List<String> findBatchIdsByStatusAndRetryCountLessThan(
            OutboxStatus status,
            Integer retryCount,
            Integer limit
    );

    List<String> findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
            OutboxStatus status,
            Instant updatedBefore,
            Integer retryCount,
            Integer limit
    );

    Integer markPublishingByBatchId(String batchId, Instant publishingAt);

    Integer markPublishedByBatchId(String batchId, Instant publishedAt);

    Integer restorePendingByBatchId(String batchId, Instant publishingAt, Instant pendingAt);

    Integer restoreExpiredPublishingByBatchId(String batchId, Instant publishingBefore, Instant pendingAt);

    void markPublishFailedByBatchId(
            String batchId,
            Instant failedAt,
            String lastError,
            Integer maxRetryCount
    );
}
