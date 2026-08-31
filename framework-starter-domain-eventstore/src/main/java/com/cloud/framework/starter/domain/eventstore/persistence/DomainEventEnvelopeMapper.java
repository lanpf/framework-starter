package com.cloud.framework.starter.domain.eventstore.persistence;

import com.cloud.framework.core.mapper.MapStructConfig;
import com.cloud.framework.domain.DomainEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, uses = DomainEventPayloadConverter.class)
public interface DomainEventEnvelopeMapper {

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", source = "domainEvent.eventType")
    @Mapping(target = "occurredAt", source = "domainEvent.occurredAt")
    @Mapping(target = "payload", source = "domainEvent")
    DomainEventEnvelope translate(Long eventId, DomainEvent domainEvent);
}
