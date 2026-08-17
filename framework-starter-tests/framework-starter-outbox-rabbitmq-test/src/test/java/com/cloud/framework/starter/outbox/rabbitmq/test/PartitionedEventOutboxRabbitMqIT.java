package com.cloud.framework.starter.outbox.rabbitmq.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import com.cloud.framework.message.support.MessageQueueSelector;
import com.cloud.framework.starter.outbox.direct.DirectIntegrationEventOutbox;
import com.cloud.framework.starter.outbox.publisher.PartitionedIntegrationEventPublisher;
import com.cloud.framework.starter.message.rabbitmq.partitioned.listener.PartitionedRabbitListener;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Header;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PartitionedEventOutboxRabbitMqIT {
    private static final String DESTINATION = "partitioned-event-outbox-test";
    private static final Integer PARTITIONS = 4;
    private static final Integer EVENTS_PER_PARTITION = 10;
    private static final Integer EVENT_COUNT = PARTITIONS * EVENTS_PER_PARTITION;
    private static final PartitionConsumptionProbe CONSUMPTION_PROBE = new PartitionConsumptionProbe();

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.2.8-management")
    );

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    @BeforeEach
    void resetProbe() {
        CONSUMPTION_PROBE.reset(PARTITIONS);
    }

    @AfterEach
    void closeContexts() {
        for (int index = this.contexts.size() - 1; index >= 0; index--) {
            this.contexts.get(index).close();
        }
        this.contexts.clear();
    }

    @Test
    void shouldRouteToFourOrderedParallelPartitionsWithOneActiveConsumerPerQueue() {
        ConfigurableApplicationContext firstContext = startContext("consumer-1");
        ConfigurableApplicationContext secondContext = startContext("consumer-2");
        assertScenarioWiring(firstContext);
        assertPartitionKeyRouting(firstContext);
        awaitFourQueuesWithTwoConsumers(firstContext);

        IntegrationEventOutbox outbox = firstContext.getBean(IntegrationEventOutbox.class);
        outbox.appendAll(events());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(CONSUMPTION_PROBE.invocationCount()).isEqualTo(EVENT_COUNT)
        );
        assertThat(CONSUMPTION_PROBE.parallelBarrierCompleted()).isTrue();
        assertThat(CONSUMPTION_PROBE.maxConcurrentCallbacks()).isGreaterThanOrEqualTo(PARTITIONS);
        for (int partition = 0; partition < PARTITIONS; partition++) {
            assertPartitionConsumption(partition);
        }
        assertThat(firstContext.isActive()).isTrue();
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
        properties.put("spring.application.name", "partitioned-event-outbox-test-" + consumerId);
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
        properties.put("framework.rabbitmq.partitioned.destinations." + DESTINATION, PARTITIONS);
        properties.put("framework.rabbitmq.partitioned.dead-letter.enabled", false);
        properties.put("test.consumer-id", consumerId);
        return properties;
    }

    private void assertScenarioWiring(ConfigurableApplicationContext context) {
        assertThat(context.getBean(IntegrationEventOutbox.class)).isInstanceOf(DirectIntegrationEventOutbox.class);
        assertThat(context.getBean(IntegrationEventPublisher.class))
                .isInstanceOf(PartitionedIntegrationEventPublisher.class);
    }

    private void assertPartitionKeyRouting(ConfigurableApplicationContext context) {
        MessageQueueSelector selector = context.getBean(MessageQueueSelector.class);
        for (int partition = 0; partition < PARTITIONS; partition++) {
            assertThat(selector.select(PARTITIONS, partitionKey(partition))).isEqualTo(partition);
        }
    }

    private void awaitFourQueuesWithTwoConsumers(ConfigurableApplicationContext context) {
        RabbitAdmin rabbitAdmin = context.getBean(RabbitAdmin.class);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            for (int partition = 0; partition < PARTITIONS; partition++) {
                String queue = PartitionedRabbitMessageSupport.queue(DESTINATION, partition);
                QueueInformation queueInformation = rabbitAdmin.getQueueInfo(queue);
                assertThat(queueInformation).isNotNull();
                assertThat(queueInformation.getConsumerCount()).isEqualTo(2);
            }
        });
    }

    private void assertPartitionConsumption(Integer partition) {
        assertThat(CONSUMPTION_PROBE.queues(partition))
                .containsExactly(PartitionedRabbitMessageSupport.queue(DESTINATION, partition));
        assertThat(CONSUMPTION_PROBE.sequences(partition))
                .containsExactlyElementsOf(IntStream.range(0, EVENTS_PER_PARTITION).boxed().toList());
        assertThat(CONSUMPTION_PROBE.consumerIds(partition)).hasSize(1);
        assertThat(CONSUMPTION_PROBE.countByPartition(partition)).isEqualTo(EVENTS_PER_PARTITION);
    }

    private List<TestIntegrationEvent> events() {
        Instant occurredAt = Instant.now();
        List<TestIntegrationEvent> events = new ArrayList<>(EVENT_COUNT);
        for (int sequence = 0; sequence < EVENTS_PER_PARTITION; sequence++) {
            for (int partition = 0; partition < PARTITIONS; partition++) {
                events.add(new TestIntegrationEvent(
                        "event-" + partition + "-" + sequence,
                        occurredAt,
                        DESTINATION,
                        partitionKey(partition),
                        partition,
                        sequence,
                        "payload-" + partition + "-" + sequence
                ));
            }
        }
        return events;
    }

    private String partitionKey(Integer partition) {
        return String.valueOf(partition);
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
        private Integer partition;
        private Integer sequence;
        private String payload;
    }

    @AllArgsConstructor
    static class TestIntegrationEventListener {
        private final String consumerId;

        @PartitionedRabbitListener(destination = DESTINATION)
        public void consume(
                TestIntegrationEvent event,
                @Header(AmqpHeaders.CONSUMER_QUEUE) String queue
        ) {
            CONSUMPTION_PROBE.record(this.consumerId, queue, event);
        }
    }

    private static class PartitionConsumptionProbe {
        private final AtomicInteger invocationCount = new AtomicInteger();
        private final AtomicInteger activeCallbacks = new AtomicInteger();
        private final AtomicInteger maxConcurrentCallbacks = new AtomicInteger();
        private final AtomicBoolean parallelBarrierCompleted = new AtomicBoolean(true);
        private final Map<Integer, List<Integer>> sequences = new ConcurrentHashMap<>();
        private final Map<Integer, Set<String>> queues = new ConcurrentHashMap<>();
        private final Map<Integer, Set<String>> consumerIds = new ConcurrentHashMap<>();
        private volatile CountDownLatch partitionStartBarrier = new CountDownLatch(0);

        void reset(Integer partitions) {
            this.invocationCount.set(0);
            this.activeCallbacks.set(0);
            this.maxConcurrentCallbacks.set(0);
            this.parallelBarrierCompleted.set(true);
            this.sequences.clear();
            this.queues.clear();
            this.consumerIds.clear();
            this.partitionStartBarrier = new CountDownLatch(partitions);
        }

        void record(String consumerId, String queue, TestIntegrationEvent event) {
            Integer active = this.activeCallbacks.incrementAndGet();
            this.maxConcurrentCallbacks.accumulateAndGet(active, Math::max);
            try {
                awaitOtherPartitions(event);
                this.sequences.computeIfAbsent(
                        event.getPartition(),
                        key -> Collections.synchronizedList(new ArrayList<>())
                ).add(event.getSequence());
                this.queues.computeIfAbsent(event.getPartition(), key -> ConcurrentHashMap.newKeySet()).add(queue);
                this.consumerIds.computeIfAbsent(
                        event.getPartition(),
                        key -> ConcurrentHashMap.newKeySet()
                ).add(consumerId);
                this.invocationCount.incrementAndGet();
            } finally {
                this.activeCallbacks.decrementAndGet();
            }
        }

        private void awaitOtherPartitions(TestIntegrationEvent event) {
            if (!Integer.valueOf(0).equals(event.getSequence())) {
                return;
            }
            this.partitionStartBarrier.countDown();
            try {
                boolean completed = this.partitionStartBarrier.await(10, TimeUnit.SECONDS);
                this.parallelBarrierCompleted.compareAndSet(true, completed);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                this.parallelBarrierCompleted.set(false);
            }
        }

        Integer invocationCount() {
            return this.invocationCount.get();
        }

        Integer maxConcurrentCallbacks() {
            return this.maxConcurrentCallbacks.get();
        }

        boolean parallelBarrierCompleted() {
            return this.parallelBarrierCompleted.get();
        }

        List<Integer> sequences(Integer partition) {
            List<Integer> values = this.sequences.get(partition);
            if (values == null) {
                return List.of();
            }
            synchronized (values) {
                return List.copyOf(values);
            }
        }

        Set<String> queues(Integer partition) {
            return Set.copyOf(this.queues.getOrDefault(partition, Set.of()));
        }

        Set<String> consumerIds(Integer partition) {
            return Set.copyOf(this.consumerIds.getOrDefault(partition, Set.of()));
        }

        Integer countByPartition(Integer partition) {
            return sequences(partition).size();
        }
    }
}
