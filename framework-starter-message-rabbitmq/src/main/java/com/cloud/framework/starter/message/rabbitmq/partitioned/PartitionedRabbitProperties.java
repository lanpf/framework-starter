package com.cloud.framework.starter.message.rabbitmq.partitioned;

import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "framework.rabbitmq.partitioned")
public class PartitionedRabbitProperties implements PartitionedRabbitTopologyRegistry {

    @NotNull
    private RoutingMode routingMode = RoutingMode.SELECTOR;

    @NotNull
    @Positive
    private Integer defaultRouteWeight = 10;

    @Valid
    private final Selector selector = new Selector();

    @Valid
    private final Listener listener = new Listener();

    @Valid
    private final DeadLetter deadLetter = new DeadLetter();

    private final Map<
            @NotBlank
            @Pattern(
                    regexp = RabbitTopologyRegistry.DESTINATION_PATTERN,
                    message = "must not contain Rabbit topic wildcards '*' or '#'"
            ) String,
            @NotNull @Positive Integer> destinations = new LinkedHashMap<>();

    private final Map<
            @NotBlank
            @Pattern(
                    regexp = RabbitTopologyRegistry.DESTINATION_PATTERN,
                    message = "must not contain Rabbit topic wildcards '*' or '#'"
            ) String,
            @NotNull @Positive Integer> weights = new LinkedHashMap<>();

    @Override
    public Set<String> destinations() {
        return Collections.unmodifiableSet(this.destinations.keySet());
    }

    @Override
    public Integer partitions(String destination) {
        Integer partitions = this.destinations.get(destination);
        if (partitions == null) {
            throw new IllegalArgumentException("partitioned rabbit destination is not configured: " + destination);
        }
        return partitions;
    }

    @Override
    public Integer weight(String destination) {
        return this.weights.getOrDefault(destination, this.defaultRouteWeight);
    }

    @Override
    public boolean deadLetterEnabled() {
        return this.deadLetter.isEnabled();
    }

    public enum RoutingMode {
        SELECTOR,
        CONSISTENT_HASH_PLUGIN
    }

    public enum SelectorAlgorithm {
        HASH,
        CONSISTENT_HASH
    }

    @Getter
    @Setter
    public static class Selector {

        @NotNull
        private SelectorAlgorithm algorithm = SelectorAlgorithm.HASH;
    }

    @Getter
    @Setter
    public static class Listener {

        @NotNull
        @Positive
        private Integer prefetch = 1;
    }

    @Getter
    @Setter
    public static class DeadLetter {

        private boolean enabled = true;
    }
}
