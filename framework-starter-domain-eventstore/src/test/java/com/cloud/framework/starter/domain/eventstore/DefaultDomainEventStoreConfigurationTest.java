package com.cloud.framework.starter.domain.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.framework.domain.AbstractDomainEvent;
import com.cloud.framework.domain.DomainEventStore;
import com.cloud.framework.id.LongIdGenerator;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelope;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DefaultDomainEventStoreConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DefaultDomainEventStoreConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(LongIdGenerator.class, () -> name -> 1L);

    @Test
    void assemblesTheDefaultStoreWithTheGeneratedMapStructMapper() {
        List<DomainEventEnvelope> storedEvents = new ArrayList<>();
        this.contextRunner
                .withBean(DomainEventEnvelopePersistenceRepository.class, () -> storedEvents::addAll)
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventStore.class);
                    Instant occurredAt = Instant.parse("2026-09-02T00:00:00Z");
                    context.getBean(DomainEventStore.class).appendAll(List.of(new TestDomainEvent(occurredAt)));

                    assertThat(storedEvents).hasSize(1);
                    DomainEventEnvelope envelope = storedEvents.get(0);
                    assertThat(envelope.eventId()).isEqualTo(1L);
                    assertThat(envelope.eventType()).isEqualTo("TestDomainEvent");
                    assertThat(envelope.occurredAt()).isEqualTo(occurredAt);
                    assertThat(envelope.payload()).contains("eventType");
                });
    }

    private static final class TestDomainEvent extends AbstractDomainEvent {
        private TestDomainEvent(Instant occurredAt) {
            super(occurredAt);
        }

        @JsonIgnore
        @Override
        public Instant getOccurredAt() {
            return super.getOccurredAt();
        }
    }
}
