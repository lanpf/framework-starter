package com.cloud.framework.starter.domain.eventstore.store;

import com.cloud.framework.domain.DomainEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "domain-event.store", name = "mode", havingValue = "noop")
public class NoopDomainEventStoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DomainEventStore noopDomainEventStore() {
        return new NoopDomainEventStore();
    }
}
