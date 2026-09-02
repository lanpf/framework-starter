package com.cloud.framework.starter.outbox.jpa.rabbitmq.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.config.JpaOutboxTestConfiguration;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaPersistenceRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaRepository;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.support.PausableAsyncTaskExecutor;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import com.cloud.framework.starter.outbox.publisher.PartitionedIntegrationEventPublisher;
import com.cloud.framework.starter.outbox.reliable.ReliableIntegrationEventOutbox;
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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ReliableEventOutboxJpaRabbitMqIT {
    private static final String DESTINATION = "reliable-event-outbox-test";
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
    void shouldPersistReliableOutboxWithJpaAndPublishToOneActiveConsumerWithConfirm() {
        ConfigurableApplicationContext firstContext = startContext("consumer-1");
        ConfigurableApplicationContext secondContext = startContext("consumer-2");
        assertScenarioWiring(firstContext);
        awaitTwoRegisteredConsumers(firstContext);

        List<TestIntegrationEvent> events = events();
        appendInTransaction(firstContext, events);

        IntegrationEventOutboxJpaRepository jpaRepository = firstContext.getBean(
                IntegrationEventOutboxJpaRepository.class
        );
        PausableAsyncTaskExecutor taskExecutor = firstContext.getBean(PausableAsyncTaskExecutor.class);
        assertPendingBatch(jpaRepository, events);
        assertThat(taskExecutor.pendingTaskCount()).isOne();
        assertThat(CONSUMPTION_PROBE.invocationCount()).isZero();
        assertQueueMessageCount(firstContext, 0);

        taskExecutor.releaseAll();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(CONSUMPTION_PROBE.invocationCount()).isEqualTo(EVENT_COUNT);
            assertThat(CONSUMPTION_PROBE.eventIds())
                    .containsExactlyInAnyOrderElementsOf(eventIds(events));
            assertPublishedBatch(jpaRepository);
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
        properties.put("spring.application.name", "reliable-event-outbox-test-" + consumerId);
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", false);
        properties.put("spring.datasource.url", "jdbc:h2:mem:reliable_outbox_" + consumerId.replace('-', '_'));
        properties.put("spring.datasource.username", "sa");
        properties.put("spring.datasource.password", "");
        properties.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.put("spring.jpa.open-in-view", false);
        properties.put("spring.rabbitmq.host", RABBITMQ.getHost());
        properties.put("spring.rabbitmq.port", RABBITMQ.getAmqpPort());
        properties.put("spring.rabbitmq.username", RABBITMQ.getAdminUsername());
        properties.put("spring.rabbitmq.password", RABBITMQ.getAdminPassword());
        properties.put("framework.outbox.integration-event.mode", "reliable");
        properties.put("framework.outbox.integration-event.retry.max-attempts", 1);
        properties.put("framework.outbox.integration-event.retry.backoff.delay", "1");
        properties.put("framework.rabbitmq.producer.reliability-mode", "publisher-confirm");
        properties.put("framework.rabbitmq.partitioned.routing-mode", "selector");
        properties.put("framework.rabbitmq.partitioned.selector.algorithm", "hash");
        properties.put("framework.rabbitmq.partitioned.destinations." + DESTINATION, 1);
        properties.put("framework.rabbitmq.partitioned.dead-letter.enabled", false);
        properties.put("test.consumer-id", consumerId);
        return properties;
    }

    private void assertScenarioWiring(ConfigurableApplicationContext context) {
        assertThat(context.getBean(IntegrationEventOutbox.class)).isInstanceOf(ReliableIntegrationEventOutbox.class);
        assertThat(context.getBean(IntegrationEventPublisher.class))
                .isInstanceOf(PartitionedIntegrationEventPublisher.class);
        assertThat(context.getBean(IntegrationEventOutboxEnvelopePersistenceRepository.class))
                .isInstanceOf(IntegrationEventOutboxJpaPersistenceRepository.class);
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

    private void appendInTransaction(
            ConfigurableApplicationContext context,
            List<TestIntegrationEvent> events
    ) {
        IntegrationEventOutbox outbox = context.getBean(IntegrationEventOutbox.class);
        PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> outbox.appendAll(events));
    }

    private void assertPendingBatch(
            IntegrationEventOutboxJpaRepository repository,
            List<TestIntegrationEvent> events
    ) {
        List<IntegrationEventOutboxDO> outboxEvents = repository.findByBatchIdOrderByBatchSequenceAsc(
                events.get(0).getEventId()
        );
        assertThat(outboxEvents).hasSize(EVENT_COUNT);
        assertThat(outboxEvents).extracting(IntegrationEventOutboxDO::getBatchId)
                .containsOnly(events.get(0).getEventId());
        assertThat(outboxEvents).extracting(IntegrationEventOutboxDO::getBatchSequence)
                .containsExactlyElementsOf(IntStream.range(0, EVENT_COUNT).boxed().toList());
        assertThat(outboxEvents).extracting(IntegrationEventOutboxDO::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxEvents).allSatisfy(outbox -> {
            assertThat(outbox.getPayload()).isNotBlank();
            assertThat(outbox.getRetryCount()).isZero();
            assertThat(outbox.getPublishedAt()).isNull();
        });
    }

    private void assertPublishedBatch(IntegrationEventOutboxJpaRepository repository) {
        List<IntegrationEventOutboxDO> outboxEvents = repository.findByBatchIdOrderByBatchSequenceAsc("event-0");
        assertThat(outboxEvents).hasSize(EVENT_COUNT);
        assertThat(outboxEvents).extracting(IntegrationEventOutboxDO::getStatus)
                .containsOnly(OutboxStatus.PUBLISHED);
        assertThat(outboxEvents).extracting(IntegrationEventOutboxDO::getPublishedAt)
                .doesNotContainNull();
    }

    private void assertQueueMessageCount(ConfigurableApplicationContext context, Integer expectedCount) {
        RabbitAdmin rabbitAdmin = context.getBean(RabbitAdmin.class);
        String queue = PartitionedRabbitMessageSupport.queue(DESTINATION, 0);
        QueueInformation queueInformation = rabbitAdmin.getQueueInfo(queue);
        assertThat(queueInformation).isNotNull();
        assertThat(queueInformation.getMessageCount()).isEqualTo(expectedCount);
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

    private List<String> eventIds(List<TestIntegrationEvent> events) {
        return events.stream().map(TestIntegrationEvent::getEventId).toList();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRabbit
    @Import(JpaOutboxTestConfiguration.class)
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
