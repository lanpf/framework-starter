package com.cloud.framework.starter.domain.eventstore.persistence;

import java.time.Instant;

public record DomainEventEnvelope(
        Long eventId,
        String eventType,
        Instant occurredAt,
        String payload
) {
}
