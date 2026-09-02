package com.cloud.framework.starter.outbox.persistence;

import java.time.Instant;

public record IntegrationEventOutboxEnvelope(
        String eventId,
        String eventType,
        String eventClassName,
        String batchId,
        Integer batchSequence,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String payload,
        OutboxStatus status,
        Integer retryCount,
        Instant publishedAt,
        Instant failedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
