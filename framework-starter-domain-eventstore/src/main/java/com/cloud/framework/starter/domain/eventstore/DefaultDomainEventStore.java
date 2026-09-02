package com.cloud.framework.starter.domain.eventstore;

import com.cloud.framework.domain.DomainEvent;
import com.cloud.framework.domain.DomainEventStore;
import com.cloud.framework.id.LongIdGenerator;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultDomainEventStore implements DomainEventStore {
    static final String ID_GENERATOR_NAME = "domain_event";

    private final DomainEventEnvelopePersistenceRepository repository;
    private final DomainEventEnvelopePersistenceMapper mapper;
    private final LongIdGenerator idGenerator;

    @Override
    public void appendAll(List<? extends DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return;
        }
        repository.saveAll(domainEvents.stream()
                .map(domainEvent -> mapper.toEnvelope(
                        idGenerator.nextId(ID_GENERATOR_NAME),
                        domainEvent))
                .toList());
    }
}
