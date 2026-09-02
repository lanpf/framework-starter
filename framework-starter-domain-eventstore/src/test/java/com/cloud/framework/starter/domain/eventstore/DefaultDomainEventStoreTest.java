package com.cloud.framework.starter.domain.eventstore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloud.framework.domain.AbstractDomainEvent;
import com.cloud.framework.id.LongIdGenerator;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelope;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class DefaultDomainEventStoreTest {

    @Test
    void shouldAssignPersistenceEnvelopeIdsWhenAppendingDomainEvents() {
        List<DomainEventEnvelope> storedEvents = new ArrayList<>();
        DomainEventEnvelopePersistenceRepository repository = storedEvents::addAll;
        DomainEventEnvelopePersistenceMapper mapper = (eventId, domainEvent) -> new DomainEventEnvelope(
                eventId,
                domainEvent.eventType(),
                domainEvent.occurredAt(),
                "payload");
        AtomicLong sequence = new AtomicLong(100L);
        LongIdGenerator idGenerator = name -> {
            assertEquals(DefaultDomainEventStore.ID_GENERATOR_NAME, name);
            return sequence.incrementAndGet();
        };
        DefaultDomainEventStore store = new DefaultDomainEventStore(repository, mapper, idGenerator);
        Instant occurredAt = Instant.parse("2026-08-31T00:00:00Z");

        store.appendAll(List.of(new TestDomainEvent(occurredAt), new TestDomainEvent(occurredAt)));

        assertEquals(List.of(101L, 102L), storedEvents.stream().map(DomainEventEnvelope::eventId).toList());
        assertEquals(List.of("TestDomainEvent", "TestDomainEvent"),
                storedEvents.stream().map(DomainEventEnvelope::eventType).toList());
    }

    private static final class TestDomainEvent extends AbstractDomainEvent {
        private TestDomainEvent(Instant occurredAt) {
            super(occurredAt);
        }
    }
}
