package com.cloud.framework.starter.outbox.persistence.mapstruct;

import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IntegrationEventOutboxPersistenceMapperConfiguration {
    @Bean
    @ConditionalOnMissingBean
    IntegrationEventPayloadConverter integrationEventPayloadConverter(ObjectMapper objectMapper) {
        return new IntegrationEventPayloadConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    IntegrationEventOutboxPersistenceMapper integrationEventOutboxPersistenceMapper() {
        return Mappers.getMapper(IntegrationEventOutboxPersistenceMapStructMapper.class);
    }
}
