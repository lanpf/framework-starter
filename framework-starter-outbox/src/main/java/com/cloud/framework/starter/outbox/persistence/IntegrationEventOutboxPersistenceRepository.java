package com.cloud.framework.starter.outbox.persistence;

import java.time.Instant;
import java.util.List;

public interface IntegrationEventOutboxPersistenceRepository {
    void saveAll(List<IntegrationEventOutboxDO> events);

    List<IntegrationEventOutboxDO> findByBatchId(String batchId);

    List<String> findBatchIdsByStatusAndRetryCountLessThan(
            OutboxStatus status,
            Integer retryCount,
            Integer limit
    );

    Integer markPublishingByBatchId(String batchId, Instant publishingAt);

    Integer markPublishedByBatchId(String batchId, Instant publishedAt);

    void markPublishFailedByBatchId(
            String batchId,
            Instant failedAt,
            String lastError,
            Integer maxRetryCount
    );
}
