package com.cloud.framework.starter.outbox.persistence.mapstruct;

import com.cloud.framework.core.mapper.MapStructConfig;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(config = MapStructConfig.class)
public abstract class IntegrationEventOutboxPersistenceMapStructMapper
        implements IntegrationEventOutboxPersistenceMapper {
    private IntegrationEventPayloadConverter payloadConverter;

    @Autowired
    void setPayloadConverter(IntegrationEventPayloadConverter payloadConverter) {
        this.payloadConverter = payloadConverter;
    }

    @Override
    @Mapping(target = "eventId", source = "event.eventId")
    @Mapping(target = "eventType", source = "event.eventType")
    @Mapping(target = "eventClassName", expression = "java(eventClassName(event))")
    @Mapping(target = "aggregateType", source = "event.aggregateType")
    @Mapping(target = "aggregateId", source = "event.aggregateId")
    @Mapping(target = "occurredAt", source = "event.occurredAt")
    @Mapping(target = "payload", expression = "java(serialize(event))")
    @Mapping(target = "status", expression = "java(outboxStatus())")
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "createdAt")
    public abstract IntegrationEventOutboxDO toDataObject(IntegrationEvent event, Instant createdAt);

    @Override
    public IntegrationEvent toIntegrationEvent(IntegrationEventOutboxDO outbox) {
        return this.payloadConverter.deserialize(outbox);
    }

    protected String serialize(IntegrationEvent event) {
        return this.payloadConverter.serialize(event);
    }

    protected String eventClassName(IntegrationEvent event) {
        return event == null ? null : event.getClass().getName();
    }

    protected OutboxStatus outboxStatus() {
        return OutboxStatus.PENDING;
    }
}
