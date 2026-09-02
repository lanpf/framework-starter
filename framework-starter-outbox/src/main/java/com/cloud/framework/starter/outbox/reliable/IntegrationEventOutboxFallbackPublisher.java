package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.starter.outbox.IntegrationEventOutboxProperties;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@RequiredArgsConstructor
public class IntegrationEventOutboxFallbackPublisher {
    private static final int MAX_ERROR_LENGTH = 1024;

    private final IntegrationEventOutboxEnvelopePersistenceRepository repository;
    private final IntegrationEventOutboxPublisher outboxPublisher;
    private final Clock clock;
    private final IntegrationEventOutboxProperties properties;

    public void publish(
            @NotNull @Positive Integer batchSize,
            @NotNull @Positive Integer maxRetryCount
    ) {
        recoverExpiredPublishingBatches(batchSize, maxRetryCount);
        List<String> batchIds =
                repository.findBatchIdsByStatusAndRetryCountLessThan(
                        OutboxStatus.PENDING,
                        maxRetryCount,
                        batchSize
                );
        int publishedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        for (String batchId : batchIds) {
            PublishAttempt attempt = publishOne(batchId, maxRetryCount);
            switch (attempt) {
                case PUBLISHED -> publishedCount++;
                case FAILED -> failedCount++;
                case SKIPPED -> skippedCount++;
            }
        }
        log.info(
                "Integration event outbox fallback finished. scanned={}, published={}, failed={}, skipped={}",
                batchIds.size(),
                publishedCount,
                failedCount,
                skippedCount
        );
    }

    private void recoverExpiredPublishingBatches(Integer batchSize, Integer maxRetryCount) {
        Instant pendingAt = clock.instant();
        Instant publishingBefore = pendingAt.minus(Duration.ofMillis(properties.getRecovery().getPublishingTimeout()));
        List<String> batchIds = repository.findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
                OutboxStatus.PUBLISHING,
                publishingBefore,
                maxRetryCount,
                batchSize
        );
        int recoveredCount = 0;
        for (String batchId : batchIds) {
            Integer restoredCount = repository.restoreExpiredPublishingByBatchId(batchId, publishingBefore, pendingAt);
            if (restoredCount != null && restoredCount > 0) {
                recoveredCount++;
            }
        }
        if (recoveredCount > 0) {
            log.info("Integration event outbox publishing lease recovered. recovered={}", recoveredCount);
        }
    }

    private PublishAttempt publishOne(String batchId, Integer maxRetryCount) {
        Instant failedAt = clock.instant();
        try {
            return outboxPublisher.publish(batchId) ? PublishAttempt.PUBLISHED : PublishAttempt.SKIPPED;
        } catch (Exception ex) {
            String lastError = truncateError(ex.getMessage());
            markPublishFailed(batchId, failedAt, lastError, maxRetryCount);
            log.warn(
                    "Integration event outbox fallback publish failed. batchId={}",
                    batchId,
                    ex
            );
            return PublishAttempt.FAILED;
        }
    }

    private void markPublishFailed(
            String batchId,
            Instant failedAt,
            String lastError,
            Integer maxRetryCount
    ) {
        try {
            repository.markPublishFailedByBatchId(batchId, failedAt, lastError, maxRetryCount);
        } catch (Exception ex) {
            log.error(
                    "Integration event outbox fallback mark failed state failed. batchId={}",
                    batchId,
                    ex
            );
        }
    }

    private String truncateError(String message) {
        if (message == null || message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }

    private enum PublishAttempt {
        PUBLISHED,
        FAILED,
        SKIPPED
    }
}
