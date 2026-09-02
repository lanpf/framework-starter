package com.cloud.framework.starter.domain.eventstore.persistence;

import com.cloud.framework.starter.domain.eventstore.persistence.mapstruct.DomainEventEnvelopeMapStructMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.mapstruct.DomainEventPayloadConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DomainEventEnvelopeConfiguration {
    @Bean
    @ConditionalOnMissingBean
    DomainEventPayloadConverter domainEventPayloadConverter(ObjectMapper objectMapper) {
        return new DomainEventPayloadConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    DomainEventEnvelopePersistenceMapper domainEventEnvelopePersistenceMapper() {
        return Mappers.getMapper(DomainEventEnvelopeMapStructMapper.class);
    }
}
