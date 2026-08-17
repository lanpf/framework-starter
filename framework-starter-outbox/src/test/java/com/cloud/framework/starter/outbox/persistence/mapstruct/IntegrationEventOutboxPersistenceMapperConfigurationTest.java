package com.cloud.framework.starter.outbox.persistence.mapstruct;

import static org.assertj.core.api.Assertions.assertThat;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IntegrationEventOutboxPersistenceMapperConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-07-21T00:00:00Z");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IntegrationEventOutboxPersistenceMapperConfiguration.class)
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
            IntegrationEventOutboxPersistenceMapper mapper =
                    context.getBean(IntegrationEventOutboxPersistenceMapper.class);
            IntegrationEventOutboxDO outbox = mapper.toDataObject(event, NOW);

            assertThat(outbox.getEventId()).isEqualTo("event-1");
            assertThat(outbox.getPayload()).isNotBlank();
        });
    }

    @Getter
    @AllArgsConstructor
    private static class TestIntegrationEvent implements IntegrationEvent {
        private final String eventId;
        private final String eventType;
        private final Instant occurredAt;
        private final String aggregateType;
        private final String aggregateId;
    }
}
