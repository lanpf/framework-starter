package com.cloud.framework.starter.domain.eventstore.store;

import com.cloud.framework.domain.DomainEvent;
import com.cloud.framework.domain.DomainEventStore;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultDomainEventStore implements DomainEventStore {
    private final DomainEventPersistenceRepository repository;
    private final DomainEventPersistenceMapper mapper;

    @Override
    public void appendAll(List<? extends DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return;
        }
        repository.saveAll(domainEvents.stream().map(mapper::toDataObject).toList());
    }
}
