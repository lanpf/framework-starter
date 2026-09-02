package com.cloud.framework.starter.outbox.persistence;

import com.cloud.framework.starter.outbox.persistence.mapstruct.IntegrationEventOutboxEnvelopeMapStructMapper;
import com.cloud.framework.starter.outbox.persistence.mapstruct.IntegrationEventOutboxPayloadConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IntegrationEventOutboxEnvelopeConfiguration {
    @Bean
    @ConditionalOnMissingBean
    IntegrationEventOutboxPayloadConverter integrationEventOutboxPayloadConverter(ObjectMapper objectMapper) {
        return new IntegrationEventOutboxPayloadConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    IntegrationEventOutboxEnvelopePersistenceMapper integrationEventOutboxPersistenceMapper() {
        return Mappers.getMapper(IntegrationEventOutboxEnvelopeMapStructMapper.class);
    }
}
