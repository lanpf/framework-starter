package com.cloud.framework.starter.lock.redis;

import com.cloud.framework.core.naming.Namespaced;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
public class RedisLockProperties implements Namespaced {

    @NotNull
    private RedisLockProviderType provider = RedisLockProviderType.SPRING_INTEGRATION;

    private String namespace;

    @NotNull
    @DurationMin(seconds = 1)
    private Duration leaseTime = Duration.ofSeconds(30);

    private boolean autoRenewal = true;

    @Valid
    private final Renewal renewal = new Renewal();


    @Getter
    @Setter
    public static class Renewal {

        @NotBlank
        private String threadNamePrefix = "redis-lock-renewal-";

        private boolean removeOnCancelPolicy = true;

        @Valid
        private final TaskExecutionProperties.Pool pool = new TaskExecutionProperties.Pool();
    }
}
