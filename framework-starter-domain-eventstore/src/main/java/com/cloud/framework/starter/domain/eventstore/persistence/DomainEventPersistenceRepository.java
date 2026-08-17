package com.cloud.framework.starter.domain.eventstore.persistence;

import java.util.List;

public interface DomainEventPersistenceRepository {
    void saveAll(List<DomainEventDO> domainEvents);
}
