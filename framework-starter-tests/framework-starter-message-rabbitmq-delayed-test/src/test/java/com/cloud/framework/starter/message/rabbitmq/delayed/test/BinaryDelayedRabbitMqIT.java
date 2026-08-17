package com.cloud.framework.starter.message.rabbitmq.delayed.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.framework.message.DelayedOperations;
import com.cloud.framework.starter.message.rabbitmq.delayed.support.DelayedRabbitMessageSupport;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Slf4j
@Testcontainers
class BinaryDelayedRabbitMqIT {

    private static final String DESTINATION = "delayed-orders-test";
    private static final String TIMESTAMP_IN_MILLIS = "timestamp_in_ms";
    private static final Duration DELAY = Duration.ofMillis(700);
    private static final Duration DELIVERY_LAG_TOLERANCE = Duration.ofSeconds(2);

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.2.8-management")
    ).withRabbitMQConfig(MountableFile.forClasspathResource("rabbitmq/rabbitmq.conf"));

    private ConfigurableApplicationContext context;

    @AfterEach
    void closeContext() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    void shouldDeliverOnlyAfterTraversingTheBinaryDelayLevels() {
        this.context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties())
                .run();
        DelayedOperations delayedOperations = this.context.getBean(DelayedOperations.class);
        RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);

        delayedOperations.convertAndSend(
                DESTINATION,
                "order-1",
                payload -> DELAY
        );

        Message earlyMessage = rabbitTemplate.receive(
                DelayedRabbitMessageSupport.deliveryQueue(DESTINATION),
                300
        );
        assertThat(earlyMessage).isNull();

        Message deliveredMessage = rabbitTemplate.receive(
                DelayedRabbitMessageSupport.deliveryQueue(DESTINATION),
                10_000
        );
        long receivedAt = System.currentTimeMillis();
        assertThat(deliveredMessage).isNotNull();
        Object payload = rabbitTemplate.getMessageConverter().fromMessage(deliveredMessage);
        assertThat(payload).isEqualTo("order-1");

        Object brokerTimestamp = deliveredMessage.getMessageProperties().getHeader(TIMESTAMP_IN_MILLIS);
        assertThat(brokerTimestamp).isInstanceOf(Number.class);
        Duration brokerDelay = Duration.ofMillis(receivedAt - ((Number) brokerTimestamp).longValue());
        assertThat(brokerDelay).isBetween(DELAY, DELAY.plus(DELIVERY_LAG_TOLERANCE));

        List<Map<String, ?>> xDeaths = deliveredMessage.getMessageProperties().getXDeathHeader();
        assertThat(xDeaths).isNotNull().hasSize(3);
        List<String> queues = xDeaths.stream().map(death -> death.get("queue").toString()).toList();
        List<String> reasons = xDeaths.stream().map(death -> death.get("reason").toString()).toList();
        List<Long> counts = xDeaths.stream()
                .map(death -> ((Number) death.get("count")).longValue())
                .toList();
        assertThat(queues).containsExactly(
                DelayedRabbitMessageSupport.levelQueue(0),
                DelayedRabbitMessageSupport.levelQueue(1),
                DelayedRabbitMessageSupport.levelQueue(2)
        );
        assertThat(reasons).containsOnly("expired");
        assertThat(counts).containsOnly(1L);
        log.info(
                "Observed RabbitMQ delayed delivery: brokerDelay={}ms, xDeathQueues={}, xDeathReasons={}, xDeathCounts={}",
                brokerDelay.toMillis(), queues, reasons, counts
        );
    }

    private Map<String, Object> properties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.application.name", "binary-delayed-rabbitmq-test");
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", false);
        properties.put("spring.rabbitmq.host", RABBITMQ.getHost());
        properties.put("spring.rabbitmq.port", RABBITMQ.getAmqpPort());
        properties.put("spring.rabbitmq.username", RABBITMQ.getAdminUsername());
        properties.put("spring.rabbitmq.password", RABBITMQ.getAdminPassword());
        properties.put("framework.rabbitmq.producer.reliability-mode", "publisher-confirm");
        properties.put("framework.rabbitmq.delayed.levels", 3);
        properties.put("framework.rabbitmq.delayed.tick-duration", "100ms");
        properties.put("framework.rabbitmq.delayed.destinations[0]", DESTINATION);
        properties.put("framework.rabbitmq.delayed.dead-letter.enabled", false);
        return properties;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}
