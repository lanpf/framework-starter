package com.cloud.framework.starter.domain.eventstore;

import com.cloud.framework.domain.DomainEventStore;
import com.cloud.framework.id.LongIdGenerator;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopeMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventEnvelopePersistenceRepository;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPayloadConverterConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(DomainEventPayloadConverterConfiguration.class)
@ConditionalOnProperty(prefix = "domain-event.store", name = "mode", havingValue = "default", matchIfMissing = true)
public class DefaultDomainEventStoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DomainEventStore domainEventStore(
            DomainEventEnvelopePersistenceRepository repository,
            DomainEventEnvelopeMapper mapper,
            LongIdGenerator idGenerator
    ) {
        return new DefaultDomainEventStore(repository, mapper, idGenerator);
    }
}
