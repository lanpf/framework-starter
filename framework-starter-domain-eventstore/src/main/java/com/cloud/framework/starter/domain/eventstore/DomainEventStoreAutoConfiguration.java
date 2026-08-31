package com.cloud.framework.starter.domain.eventstore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        DefaultDomainEventStoreConfiguration.class,
        NoopDomainEventStoreConfiguration.class
})
public class DomainEventStoreAutoConfiguration {
}
