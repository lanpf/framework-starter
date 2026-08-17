package com.cloud.framework.starter.domain.eventstore.persistence.mapstruct;

import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DomainEventPersistenceMapperConfiguration {
    @Bean
    @ConditionalOnMissingBean
    DomainEventPayloadConverter domainEventPayloadConverter(ObjectMapper objectMapper) {
        return new DomainEventPayloadConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    DomainEventPersistenceMapper domainEventPersistenceMapper() {
        return Mappers.getMapper(DomainEventPersistenceMapStructMapper.class);
    }
}
