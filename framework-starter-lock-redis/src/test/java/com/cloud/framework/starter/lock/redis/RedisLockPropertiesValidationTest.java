package com.cloud.framework.starter.lock.redis;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisLockPropertiesValidationTest {

    private final Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Test
    void shouldAllowMissingNamespace() {
        RedisLockProperties properties = new RedisLockProperties();

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void shouldRequirePositiveLeaseTime() {
        RedisLockProperties properties = new RedisLockProperties();
        properties.setNamespace("order-service");
        properties.setLeaseTime(Duration.ZERO);

        assertEquals(1, validator.validate(properties).size());
    }
}
