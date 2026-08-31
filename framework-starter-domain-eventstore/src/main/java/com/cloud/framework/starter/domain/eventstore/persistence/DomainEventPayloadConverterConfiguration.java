package com.cloud.framework.starter.domain.eventstore.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DomainEventPayloadConverterConfiguration {
    @Bean
    @ConditionalOnMissingBean
    DomainEventPayloadConverter domainEventPayloadConverter(ObjectMapper objectMapper) {
        return new DomainEventPayloadConverter(objectMapper);
    }
}
