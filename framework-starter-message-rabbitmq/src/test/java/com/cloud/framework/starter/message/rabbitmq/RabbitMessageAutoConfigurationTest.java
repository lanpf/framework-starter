package com.cloud.framework.starter.message.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMessageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitMessageAutoConfiguration.class));

    @Test
    void shouldStartWithoutDefaultSpringMessageConverter() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("customRabbitListenerConfigurer");
        });
    }

    @Test
    void shouldRejectNonPositivePublisherConfirmTimeout() {
        this.contextRunner
                .withPropertyValues("framework.rabbitmq.producer.confirm-timeout=0s")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void shouldSetJsonContentTypeWhenConvertingPayload() {
        MessageConverter converter = new RabbitMessageAutoConfiguration().rabbitMessageConverter(
                new StaticListableBeanFactory().getBeanProvider(ObjectMapper.class)
        );

        Message message = converter.toMessage(Map.of("eventId", "event-1"), new MessageProperties());

        assertThat(message.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }
}
