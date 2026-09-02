package com.cloud.framework.starter.outbox.persistence.mapstruct;

import com.cloud.framework.core.mapper.MapStructConfig;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceMapper;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, uses = IntegrationEventOutboxPayloadConverter.class)
public interface IntegrationEventOutboxEnvelopeMapStructMapper
        extends IntegrationEventOutboxEnvelopePersistenceMapper {

    @Override
    @Mapping(target = "eventId", source = "event.eventId")
    @Mapping(target = "eventType", source = "event.eventType")
    @Mapping(target = "eventClassName", expression = "java(event == null ? null : event.getClass().getName())")
    @Mapping(target = "batchId", source = "batchId")
    @Mapping(target = "batchSequence", source = "batchSequence")
    @Mapping(target = "aggregateType", source = "event.aggregateType")
    @Mapping(target = "aggregateId", source = "event.aggregateId")
    @Mapping(target = "occurredAt", source = "event.occurredAt")
    @Mapping(target = "payload", source = "event")
    @Mapping(target = "status", expression = "java(OutboxStatus.PENDING)")
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "createdAt")
    IntegrationEventOutboxEnvelope toEnvelope(
            IntegrationEvent event,
            Instant createdAt,
            String batchId,
            Integer batchSequence
    );
}
