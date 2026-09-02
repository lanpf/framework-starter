package com.cloud.framework.starter.domain.eventstore.persistence;

import com.cloud.framework.domain.DomainEvent;

public interface DomainEventEnvelopePersistenceMapper {

    DomainEventEnvelope toEnvelope(Long eventId, DomainEvent domainEvent);
}
