package com.cloud.framework.starter.outbox.persistence.mapstruct;

import static org.assertj.core.api.Assertions.assertThat;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopeConfiguration;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IntegrationEventOutboxEnvelopePersistenceMapperConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IntegrationEventOutboxEnvelopeConfiguration.class)
            .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());

    @Test
    void injectsThePayloadConverterIntoTheMapStructMapper() {
        IntegrationEvent event = new TestIntegrationEvent(
                "event-1",
                "TestEvent",
                NOW,
                "TestAggregate",
                "aggregate-1"
        );

        this.contextRunner.run(context -> {
            IntegrationEventOutboxEnvelopePersistenceMapper mapper =
                    context.getBean(IntegrationEventOutboxEnvelopePersistenceMapper.class);
            IntegrationEventOutboxEnvelope envelope = mapper.toEnvelope(event, NOW, "event-1", 0);

            assertThat(envelope.eventId()).isEqualTo("event-1");
            assertThat(envelope.eventType()).isEqualTo("TestEvent");
            assertThat(envelope.eventClassName()).isEqualTo(TestIntegrationEvent.class.getName());
            assertThat(envelope.batchId()).isEqualTo("event-1");
            assertThat(envelope.batchSequence()).isZero();
            assertThat(envelope.aggregateType()).isEqualTo("TestAggregate");
            assertThat(envelope.aggregateId()).isEqualTo("aggregate-1");
            assertThat(envelope.occurredAt()).isEqualTo(NOW);
            assertThat(envelope.payload()).isNotBlank();
            assertThat(envelope.status()).isEqualTo(OutboxStatus.PENDING);
            assertThat(envelope.retryCount()).isZero();
            assertThat(envelope.createdAt()).isEqualTo(NOW);
            assertThat(envelope.updatedAt()).isEqualTo(NOW);

            IntegrationEventOutboxPayloadConverter converter =
                    context.getBean(IntegrationEventOutboxPayloadConverter.class);
            IntegrationEvent restored = converter.deserialize(envelope);
            assertThat(restored).isInstanceOfSatisfying(TestIntegrationEvent.class, restoredEvent -> {
                assertThat(restoredEvent.getEventId()).isEqualTo("event-1");
                assertThat(restoredEvent.getEventType()).isEqualTo("TestEvent");
                assertThat(restoredEvent.getOccurredAt()).isEqualTo(NOW);
                assertThat(restoredEvent.getAggregateType()).isEqualTo("TestAggregate");
                assertThat(restoredEvent.getAggregateId()).isEqualTo("aggregate-1");
            });
        });
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestIntegrationEvent implements IntegrationEvent {
        private String eventId;
        private String eventType;
        private Instant occurredAt;
        private String aggregateType;
        private String aggregateId;
    }
}
