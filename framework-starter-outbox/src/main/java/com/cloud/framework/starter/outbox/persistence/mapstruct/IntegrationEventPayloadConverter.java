package com.cloud.framework.starter.outbox.persistence.mapstruct;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class IntegrationEventPayloadConverter {
    private final ObjectMapper objectMapper;

    String serialize(IntegrationEvent event) {
        if (event == null) {
            return null;
        }
        try {
            return objectMapper.writerFor(event.getClass()).writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize " + event.getClass().getSimpleName() + ".", ex);
        }
    }

    IntegrationEvent deserialize(IntegrationEventOutboxDO outbox) {
        if (outbox == null || outbox.getPayload() == null) {
            return null;
        }
        try {
            Class<?> eventClass = Class.forName(outbox.getEventClassName());
            return (IntegrationEvent) objectMapper.readValue(outbox.getPayload(), eventClass);
        } catch (ClassNotFoundException | JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to deserialize " + simpleName(outbox.getEventClassName()) + ".",
                    ex
            );
        }
    }

    private String simpleName(String className) {
        if (className == null || className.isBlank()) {
            return "payload";
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == className.length() - 1) {
            return className;
        }
        return className.substring(lastDotIndex + 1);
    }
}
