package com.cloud.framework.starter.outbox.persistence.mapstruct;

import com.cloud.framework.core.error.FrameworkError;
import com.cloud.framework.core.error.FrameworkException;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class IntegrationEventOutboxPayloadConverter {
    private final ObjectMapper objectMapper;

    public String serialize(IntegrationEvent event) {
        if (event == null) {
            return null;
        }
        try {
            return objectMapper.writerFor(event.getClass()).writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new FrameworkException(
                    FrameworkError.SERIALIZATION_FAILED,
                    "Failed to serialize " + event.getClass().getSimpleName() + ".",
                    ex
            );
        }
    }

    public IntegrationEvent deserialize(IntegrationEventOutboxEnvelope envelope) {
        if (envelope == null || envelope.payload() == null) {
            return null;
        }
        try {
            Class<?> eventClass = Class.forName(envelope.eventClassName());
            return (IntegrationEvent) objectMapper.readValue(envelope.payload(), eventClass);
        } catch (ClassNotFoundException | JsonProcessingException ex) {
            throw new FrameworkException(
                    FrameworkError.SERIALIZATION_FAILED,
                    "Failed to deserialize " + simpleName(envelope.eventClassName()) + ".",
                    ex
            );
        }
    }

    private String simpleName(String className) {
        if (!StringUtils.hasText(className)) {
            return "payload";
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == className.length() - 1) {
            return className;
        }
        return className.substring(lastDotIndex + 1);
    }
}
