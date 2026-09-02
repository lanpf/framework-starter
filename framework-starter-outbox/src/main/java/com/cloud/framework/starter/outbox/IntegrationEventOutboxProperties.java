package com.cloud.framework.starter.outbox;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Validated
@ConfigurationProperties(prefix = "framework.outbox.integration-event")
public class IntegrationEventOutboxProperties {
    @Valid
    private final Async async = new Async();

    @Valid
    private final Retry retry = new Retry();

    @Valid
    private final Recovery recovery = new Recovery();

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
        @Positive
        private Long delay = 1000L;

        @NotNull
        @PositiveOrZero
        private Long maxDelay = 0L;

        @NotNull
        @DecimalMin("0.0")
        private Double multiplier = 0.0;

        private boolean random;
    }

    @Getter
    @Setter
    public static class Recovery {
        @NotNull
        @Positive
        private Long publishingTimeout = 5 * 60 * 1000L;
    }
}
