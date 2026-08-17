package com.cloud.framework.starter.domain.eventstore.persistence.mapstruct;

import com.cloud.framework.core.mapper.MapStructConfig;
import com.cloud.framework.domain.DomainEvent;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventDO;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, uses = DomainEventPayloadConverter.class)
public abstract class DomainEventPersistenceMapStructMapper implements DomainEventPersistenceMapper {
    @Override
    @Mapping(target = "eventId", source = "eventId.value")
    @Mapping(target = "payload", source = "domainEvent")
    public abstract DomainEventDO toDataObject(DomainEvent domainEvent);
}
