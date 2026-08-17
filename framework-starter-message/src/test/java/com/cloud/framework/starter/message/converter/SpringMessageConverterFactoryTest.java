package com.cloud.framework.starter.message.converter;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringMessageConverterFactoryTest {

    @Test
    void shouldCreateDefaultSpringMessageConverters() {
        CompositeMessageConverter converter = assertInstanceOf(
                CompositeMessageConverter.class,
                SpringMessageConverterFactory.create()
        );
        List<MessageConverter> converters = converter.getConverters();

        assertTrue(converters.stream().anyMatch(ByteArrayMessageConverter.class::isInstance));
        assertTrue(converters.stream().anyMatch(StringMessageConverter.class::isInstance));
        assertTrue(converters.stream().anyMatch(MappingJackson2MessageConverter.class::isInstance));
    }
}
