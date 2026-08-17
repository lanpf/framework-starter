package com.cloud.framework.starter.outbox;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "framework.outbox.integration-event")
public class IntegrationEventOutboxProperties {
    @Valid
    private final Async async = new Async();

    @Valid
    private final Retry retry = new Retry();

    @Getter
    @Setter
    public static class Async {
        @NotBlank
        private String threadNamePrefix = "integration-event-outbox-";

        @Valid
        private final TaskExecutionProperties.Pool pool = new TaskExecutionProperties.Pool();
    }

    @Getter
    @Setter
    public static class Retry {
        @NotNull
        @Positive
        private Integer maxAttempts = 3;

        @Valid
        private final Backoff backoff = new Backoff();

        private final List<@NotNull Class<? extends Throwable>> retryFor = new ArrayList<>();

        private final List<@NotNull Class<? extends Throwable>> noRetryFor = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Backoff {
        @NotNull
        private Duration delay = Duration.ofSeconds(1);

        @NotNull
        private Duration maxDelay = Duration.ZERO;

        @NotNull
        @DecimalMin("0.0")
        private Double multiplier = 0.0;

        private boolean random;

        @AssertTrue(message = "outbox retry backoff delay must be at least one millisecond")
        public boolean isDelayValid() {
            return this.delay != null && this.delay.toMillis() > 0;
        }

        @AssertTrue(message = "outbox retry backoff max-delay must be zero or at least one millisecond")
        public boolean isMaxDelayValid() {
            return this.maxDelay != null
                    && (this.maxDelay.isZero() || this.maxDelay.toMillis() > 0);
        }
    }
}
