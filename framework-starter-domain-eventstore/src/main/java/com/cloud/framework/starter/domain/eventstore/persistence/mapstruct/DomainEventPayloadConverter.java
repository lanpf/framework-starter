package com.cloud.framework.starter.domain.eventstore.persistence.mapstruct;

import com.cloud.framework.kernel.error.FrameworkException;
import com.cloud.framework.domain.DomainEvent;
import com.cloud.framework.starter.domain.eventstore.error.DomainEventStoreError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainEventPayloadConverter {
    private final ObjectMapper objectMapper;

    public String serialize(DomainEvent domainEvent) {
        if (domainEvent == null) {
            return null;
        }
        try {
            return objectMapper.writerFor(domainEvent.getClass()).writeValueAsString(domainEvent);
        } catch (JsonProcessingException ex) {
            throw new FrameworkException(
                    DomainEventStoreError.SERIALIZATION_FAILED,
                    "Failed to serialize " + domainEvent.getClass().getSimpleName() + ".",
                    ex
            );
        }
    }
}
