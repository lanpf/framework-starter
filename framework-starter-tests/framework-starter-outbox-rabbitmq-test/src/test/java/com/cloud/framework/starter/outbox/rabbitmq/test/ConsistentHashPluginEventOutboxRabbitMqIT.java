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
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.ConsistentHashPluginPartitionedRabbitTemplate;
import com.cloud.framework.starter.message.rabbitmq.partitioned.template.PartitionedRabbitTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class ConsistentHashPluginEventOutboxRabbitMqIT {

    private static final String CONSISTENT_HASH_PLUGIN = "rabbitmq_consistent_hash_exchange";
    private static final String DESTINATION = "consistent-hash-event-outbox-test";
    private static final Integer PARTITIONS = 4;
    private static final Integer PARTITION_KEY_COUNT = 64;
    private static final Integer EVENTS_PER_PARTITION_KEY = 5;
    private static final Integer EVENT_COUNT = PARTITION_KEY_COUNT * EVENTS_PER_PARTITION_KEY;
    private static final ConsumptionProbe CONSUMPTION_PROBE = new ConsumptionProbe();

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.2.8-management")
    ).withCopyFileToContainer(
            MountableFile.forClasspathResource("rabbitmq/enabled_plugins"),
            "/etc/rabbitmq/enabled_plugins"
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
    void shouldKeepEachPartitionKeyOnOneOrderedQueueWithConsistentHashPlugin() throws Exception {
        assertPluginEnabled();
        ConfigurableApplicationContext firstContext = startContext("consumer-1");
        ConfigurableApplicationContext secondContext = startContext("consumer-2");
        assertScenarioWiring(firstContext);
        awaitFourQueuesWithTwoConsumers(firstContext);

        IntegrationEventOutbox outbox = firstContext.getBean(IntegrationEventOutbox.class);
        outbox.appendAll(events());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(CONSUMPTION_PROBE.invocationCount()).isEqualTo(EVENT_COUNT)
        );
        assertPartitionKeyAffinityAndOrder();
        assertAllQueuesUsedByOneActiveConsumer();
        assertThat(secondContext.isActive()).isTrue();
    }

    private void assertPluginEnabled() throws Exception {
        ExecResult result = RABBITMQ.execInContainer(
                "rabbitmq-plugins",
                "is_enabled",
                CONSISTENT_HASH_PLUGIN
        );
        assertThat(result.getExitCode()).isZero();
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
        properties.put("spring.application.name", "consistent-hash-event-outbox-test-" + consumerId);
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", false);
        properties.put("spring.rabbitmq.host", RABBITMQ.getHost());
        properties.put("spring.rabbitmq.port", RABBITMQ.getAmqpPort());
        properties.put("spring.rabbitmq.username", RABBITMQ.getAdminUsername());
        properties.put("spring.rabbitmq.password", RABBITMQ.getAdminPassword());
        properties.put("framework.outbox.integration-event.mode", "direct");
        properties.put("framework.rabbitmq.producer.reliability-mode", "publisher-confirm");
        properties.put("framework.rabbitmq.partitioned.routing-mode", "consistent-hash-plugin");
        properties.put("framework.rabbitmq.partitioned.destinations." + DESTINATION, PARTITIONS);
        properties.put("framework.rabbitmq.partitioned.dead-letter.enabled", false);
        properties.put("test.consumer-id", consumerId);
        return properties;
    }

    private void assertScenarioWiring(ConfigurableApplicationContext context) {
        assertThat(context.getBean(IntegrationEventOutbox.class)).isInstanceOf(DirectIntegrationEventOutbox.class);
        assertThat(context.getBean(IntegrationEventPublisher.class))
                .isInstanceOf(PartitionedIntegrationEventPublisher.class);
        assertThat(context.getBean(PartitionedRabbitTemplate.class))
                .isInstanceOf(ConsistentHashPluginPartitionedRabbitTemplate.class);
        assertThat(context.getBeansOfType(MessageQueueSelector.class)).isEmpty();
    }

    private void awaitFourQueuesWithTwoConsumers(ConfigurableApplicationContext context) {
        RabbitAdmin rabbitAdmin = context.getBean(RabbitAdmin.class);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            for (int partition = 0; partition < PARTITIONS; partition++) {
                QueueInformation queueInformation = rabbitAdmin.getQueueInfo(
                        PartitionedRabbitMessageSupport.queue(DESTINATION, partition)
                );
                assertThat(queueInformation).isNotNull();
                assertThat(queueInformation.getConsumerCount()).isEqualTo(2);
            }
        });
    }

    private List<TestIntegrationEvent> events() {
        Instant occurredAt = Instant.now();
        List<TestIntegrationEvent> events = new ArrayList<>(EVENT_COUNT);
        for (int sequence = 0; sequence < EVENTS_PER_PARTITION_KEY; sequence++) {
            for (int keyIndex = 0; keyIndex < PARTITION_KEY_COUNT; keyIndex++) {
                String partitionKey = partitionKey(keyIndex);
                events.add(new TestIntegrationEvent(
                        "event-" + keyIndex + "-" + sequence,
                        occurredAt,
                        DESTINATION,
                        partitionKey,
                        sequence
                ));
            }
        }
        return events;
    }

    private void assertPartitionKeyAffinityAndOrder() {
        List<Integer> expectedSequences = IntStream.range(0, EVENTS_PER_PARTITION_KEY).boxed().toList();
        for (int keyIndex = 0; keyIndex < PARTITION_KEY_COUNT; keyIndex++) {
            String partitionKey = partitionKey(keyIndex);
            assertThat(CONSUMPTION_PROBE.queues(partitionKey)).hasSize(1);
            assertThat(CONSUMPTION_PROBE.sequences(partitionKey)).containsExactlyElementsOf(expectedSequences);
        }
    }

    private void assertAllQueuesUsedByOneActiveConsumer() {
        Set<String> expectedQueues = new LinkedHashSet<>();
        for (int partition = 0; partition < PARTITIONS; partition++) {
            expectedQueues.add(PartitionedRabbitMessageSupport.queue(DESTINATION, partition));
        }
        assertThat(CONSUMPTION_PROBE.usedQueues()).containsExactlyInAnyOrderElementsOf(expectedQueues);
        expectedQueues.forEach(queue -> assertThat(CONSUMPTION_PROBE.consumerIds(queue)).hasSize(1));
    }

    private String partitionKey(Integer index) {
        return "aggregate-" + index;
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
        private Integer sequence;
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

    private static class ConsumptionProbe {
        private final AtomicInteger invocationCount = new AtomicInteger();
        private final Map<String, List<Integer>> sequences = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> queues = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> consumerIds = new ConcurrentHashMap<>();

        void reset() {
            this.invocationCount.set(0);
            this.sequences.clear();
            this.queues.clear();
            this.consumerIds.clear();
        }

        void record(String consumerId, String queue, TestIntegrationEvent event) {
            this.invocationCount.incrementAndGet();
            this.sequences.computeIfAbsent(
                    event.getAggregateId(),
                    key -> Collections.synchronizedList(new ArrayList<>())
            ).add(event.getSequence());
            this.queues.computeIfAbsent(event.getAggregateId(), key -> ConcurrentHashMap.newKeySet()).add(queue);
            this.consumerIds.computeIfAbsent(queue, key -> ConcurrentHashMap.newKeySet()).add(consumerId);
        }

        Integer invocationCount() {
            return this.invocationCount.get();
        }

        List<Integer> sequences(String partitionKey) {
            return List.copyOf(this.sequences.getOrDefault(partitionKey, List.of()));
        }

        Set<String> queues(String partitionKey) {
            return Set.copyOf(this.queues.getOrDefault(partitionKey, Set.of()));
        }

        Set<String> usedQueues() {
            return Set.copyOf(this.consumerIds.keySet());
        }

        Set<String> consumerIds(String queue) {
            return Set.copyOf(this.consumerIds.getOrDefault(queue, Set.of()));
        }
    }
}
