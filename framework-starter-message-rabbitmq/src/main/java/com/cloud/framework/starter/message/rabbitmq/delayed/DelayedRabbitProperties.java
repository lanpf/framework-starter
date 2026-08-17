package com.cloud.framework.starter.message.rabbitmq.delayed;

import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "framework.rabbitmq.delayed")
public class DelayedRabbitProperties implements DelayedRabbitTopologyRegistry {

    @NotNull
    @Min(1)
    @Max(40)
    private Integer levels = 24;

    @NotNull
    private Duration tickDuration = Duration.ofSeconds(1);

    private boolean quorum = true;

    private boolean atLeastOnce = true;

    private final Set<
            @NotBlank
            @Pattern(
                    regexp = RabbitTopologyRegistry.DESTINATION_PATTERN,
                    message = "must not contain Rabbit topic wildcards '*' or '#'"
            ) String> destinations = new LinkedHashSet<>();

    @Valid
    private final DeadLetter deadLetter = new DeadLetter();

    @Override
    public Set<String> destinations() {
        return Collections.unmodifiableSet(this.destinations);
    }

    @Override
    public Integer levels() {
        return this.levels;
    }

    @Override
    public Duration tickDuration() {
        return this.tickDuration;
    }

    @Override
    public boolean quorum() {
        return this.quorum;
    }

    @Override
    public boolean atLeastOnce() {
        return this.atLeastOnce;
    }

    @Override
    public boolean deadLetterEnabled() {
        return this.deadLetter.isEnabled();
    }

    @AssertTrue(message = "tick-duration must be at least one millisecond")
    public boolean isTickDurationValid() {
        return this.tickDuration == null || this.tickDuration.toMillis() > 0;
    }

    @AssertTrue(message = "at-least-once delayed delivery requires quorum queues")
    public boolean isAtLeastOnceValid() {
        return !this.atLeastOnce || this.quorum;
    }

    @AssertTrue(message = "levels and tick-duration exceed the supported TTL range")
    public boolean isMaximumLevelTtlValid() {
        if (this.levels == null || this.tickDuration == null || this.tickDuration.toMillis() <= 0) {
            return true;
        }
        try {
            Math.multiplyExact(this.tickDuration.toMillis(), 1L << (this.levels - 1));
            return true;
        } catch (ArithmeticException ex) {
            return false;
        }
    }

    @Getter
    @Setter
    public static class DeadLetter {

        private boolean enabled = true;
    }
}
