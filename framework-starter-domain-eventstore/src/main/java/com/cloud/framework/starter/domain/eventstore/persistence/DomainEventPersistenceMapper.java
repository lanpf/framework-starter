package com.cloud.framework.starter.domain.eventstore.persistence;

import com.cloud.framework.domain.DomainEvent;

public interface DomainEventPersistenceMapper {
    DomainEventDO toDataObject(DomainEvent domainEvent);
}
