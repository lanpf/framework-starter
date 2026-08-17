package com.cloud.framework.starter.domain.eventstore.store;

import com.cloud.framework.domain.DomainEventStore;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceMapper;
import com.cloud.framework.starter.domain.eventstore.persistence.DomainEventPersistenceRepository;
import com.cloud.framework.starter.domain.eventstore.persistence.mapstruct.DomainEventPersistenceMapperConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(DomainEventPersistenceMapperConfiguration.class)
@ConditionalOnProperty(prefix = "domain-event.store", name = "mode", havingValue = "default", matchIfMissing = true)
public class DefaultDomainEventStoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DomainEventStore domainEventStore(
            DomainEventPersistenceRepository repository,
            DomainEventPersistenceMapper mapper
    ) {
        return new DefaultDomainEventStore(repository, mapper);
    }
}
