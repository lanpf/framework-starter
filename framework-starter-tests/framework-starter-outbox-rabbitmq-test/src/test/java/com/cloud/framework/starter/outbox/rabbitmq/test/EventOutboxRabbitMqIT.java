package com.cloud.framework.starter.outbox.rabbitmq.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.direct.DirectIntegrationEventOutbox;
import com.cloud.framework.starter.outbox.publisher.PartitionedIntegrationEventPublisher;
import com.cloud.framework.starter.message.rabbitmq.partitioned.listener.PartitionedRabbitListener;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class EventOutboxRabbitMqIT {

    private static final String DESTINATION = "event-outbox-test";
    private static final Integer EVENT_COUNT = 20;
    private static final ConsumptionProbe CONSUMPTION_PROBE = new ConsumptionProbe();

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.2.8-management")
    );

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    @BeforeEach
    void resetProbe() {
        CONSUMPTION_PROBE.reset();
    }

    @AfterEach
    void closeContexts() {
        for (int index = this.contexts.size() - 1; index >= 0; index--) {
            this.contexts.get(index).close();
        }
        this.contexts.clear();
    }

    @Test
    void shouldPublishDirectOutboxWithConfirmAndConsumeByOneActiveConsumer() {
        ConfigurableApplicationContext firstContext = startContext("consumer-1");
        ConfigurableApplicationContext secondContext = startContext("consumer-2");
        assertScenarioWiring(firstContext);
        awaitTwoRegisteredConsumers(firstContext);

        IntegrationEventOutbox outbox = firstContext.getBean(IntegrationEventOutbox.class);
        outbox.appendAll(events());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(CONSUMPTION_PROBE.invocationCount()).isEqualTo(EVENT_COUNT);
            assertThat(CONSUMPTION_PROBE.eventIds()).hasSize(EVENT_COUNT);
        });
        assertThat(CONSUMPTION_PROBE.consumerIds()).hasSize(1);
        assertThat(CONSUMPTION_PROBE.countByConsumer().values()).containsExactly(EVENT_COUNT);
        assertThat(secondContext.isActive()).isTrue();
    }

    private ConfigurableApplicationContext startContext(String consumerId) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties(consumerId))
                .run();
        this.contexts.add(context);
        return context;
    }

    private Map<String, Object> properties(String consumerId) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.application.name", "event-outbox-rabbitmq-test-" + consumerId);
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", false);
        properties.put("spring.rabbitmq.host", RABBITMQ.getHost());
        properties.put("spring.rabbitmq.port", RABBITMQ.getAmqpPort());
        properties.put("spring.rabbitmq.username", RABBITMQ.getAdminUsername());
        properties.put("spring.rabbitmq.password", RABBITMQ.getAdminPassword());
        properties.put("framework.outbox.integration-event.mode", "direct");
        properties.put("framework.rabbitmq.producer.reliability-mode", "publisher-confirm");
        properties.put("framework.rabbitmq.partitioned.routing-mode", "selector");
        properties.put("framework.rabbitmq.partitioned.selector.algorithm", "hash");
        properties.put("framework.rabbitmq.partitioned.destinations." + DESTINATION, 1);
        properties.put("framework.rabbitmq.partitioned.dead-letter.enabled", false);
        properties.put("test.consumer-id", consumerId);
        return properties;
    }

    private void assertScenarioWiring(ConfigurableApplicationContext context) {
        assertThat(context.getBean(IntegrationEventOutbox.class)).isInstanceOf(DirectIntegrationEventOutbox.class);
        assertThat(context.getBean(IntegrationEventPublisher.class))
                .isInstanceOf(PartitionedIntegrationEventPublisher.class);
    }

    private void awaitTwoRegisteredConsumers(ConfigurableApplicationContext context) {
        RabbitAdmin rabbitAdmin = context.getBean(RabbitAdmin.class);
        String queue = PartitionedRabbitMessageSupport.queue(DESTINATION, 0);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            QueueInformation queueInformation = rabbitAdmin.getQueueInfo(queue);
            assertThat(queueInformation).isNotNull();
            assertThat(queueInformation.getConsumerCount()).isEqualTo(2);
        });
    }

    private List<TestIntegrationEvent> events() {
        Instant occurredAt = Instant.now();
        return IntStream.range(0, EVENT_COUNT)
                .mapToObj(index -> new TestIntegrationEvent(
                        "event-" + index,
                        occurredAt,
                        DESTINATION,
                        "aggregate-1",
                        "payload-" + index
                ))
                .toList();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRabbit
    static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }

        @Bean
        TestIntegrationEventListener testIntegrationEventListener(
                @Value("${test.consumer-id}") String consumerId
        ) {
            return new TestIntegrationEventListener(consumerId);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestIntegrationEvent implements IntegrationEvent {
        private String eventId;
        private Instant occurredAt;
        private String aggregateType;
        private String aggregateId;
        private String payload;
    }

    @AllArgsConstructor
    static class TestIntegrationEventListener {
        private final String consumerId;

        @PartitionedRabbitListener(destination = DESTINATION)
        public void consume(TestIntegrationEvent event) {
            CONSUMPTION_PROBE.record(this.consumerId, event.getEventId());
        }
    }

    private static class ConsumptionProbe {
        private final AtomicInteger invocationCount = new AtomicInteger();
        private final Set<String> eventIds = ConcurrentHashMap.newKeySet();
        private final Map<String, AtomicInteger> countByConsumer = new ConcurrentHashMap<>();

        void reset() {
            this.invocationCount.set(0);
            this.eventIds.clear();
            this.countByConsumer.clear();
        }

        void record(String consumerId, String eventId) {
            this.invocationCount.incrementAndGet();
            this.eventIds.add(eventId);
            this.countByConsumer.computeIfAbsent(consumerId, key -> new AtomicInteger()).incrementAndGet();
        }

        Integer invocationCount() {
            return this.invocationCount.get();
        }

        Set<String> eventIds() {
            return Set.copyOf(this.eventIds);
        }

        Set<String> consumerIds() {
            return Set.copyOf(this.countByConsumer.keySet());
        }

        Map<String, Integer> countByConsumer() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            this.countByConsumer.forEach((consumerId, count) -> counts.put(consumerId, count.get()));
            return counts;
        }
    }
}
