package com.cloud.framework.starter.message.rabbitmq.delayed;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayedRabbitPropertiesValidationTest {

    private final Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Test
    void shouldValidateTickDurationAndLevels() {
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.setLevels(0);
        properties.setTickDuration(Duration.ZERO);

        assertEquals(2, this.validator.validate(properties).size());
    }

    @Test
    void shouldRequireQuorumForAtLeastOnceDeadLettering() {
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.setQuorum(false);

        assertEquals(1, this.validator.validate(properties).size());
    }

    @Test
    void shouldValidateDestinationElements() {
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.getDestinations().add(" ");

        assertEquals(1, this.validator.validate(properties).size());
    }

    @Test
    void shouldRejectTopicWildcardsButAllowQualifiedDestinations() {
        DelayedRabbitProperties properties = new DelayedRabbitProperties();
        properties.getDestinations().add("foo.orders");
        properties.getDestinations().add("orders.*");

        assertEquals(1, this.validator.validate(properties).size());
    }
}
