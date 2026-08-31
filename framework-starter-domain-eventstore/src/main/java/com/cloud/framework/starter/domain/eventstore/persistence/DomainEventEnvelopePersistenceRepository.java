package com.cloud.framework.starter.domain.eventstore.persistence;

import java.util.List;

public interface DomainEventEnvelopePersistenceRepository {
    void saveAll(List<DomainEventEnvelope> domainEvents);
}
