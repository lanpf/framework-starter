package com.cloud.framework.starter.message.rabbitmq;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Validated
@ConfigurationProperties(prefix = "framework.rabbitmq")
public class RabbitMessageProperties {

    @Valid
    private final Producer producer = new Producer();

    @Valid
    private final Consumer consumer = new Consumer();

    public enum ProducerReliabilityMode {
        TRANSACTION,
        PUBLISHER_CONFIRM
    }

    public enum ConsumerReliabilityMode {
        TRANSACTION
    }

    @Getter
    @Setter
    public static class Producer {

        private ProducerReliabilityMode reliabilityMode;

        @NotNull
        private Duration confirmTimeout = Duration.ofSeconds(5);

        @AssertTrue(message = "framework.rabbitmq.producer.confirm-timeout must be positive")
        public boolean isConfirmTimeoutValid() {
            return this.confirmTimeout != null && !this.confirmTimeout.isZero() && !this.confirmTimeout.isNegative();
        }
    }

    @Getter
    @Setter
    public static class Consumer {

        private ConsumerReliabilityMode reliabilityMode;
    }
}
