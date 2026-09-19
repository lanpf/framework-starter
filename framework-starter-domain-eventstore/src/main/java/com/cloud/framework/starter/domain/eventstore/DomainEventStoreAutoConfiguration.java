package com.cloud.framework.starter.domain.eventstore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        DefaultDomainEventStoreAutoConfiguration.class,
        NoopDomainEventStoreAutoConfiguration.class
})
public class DomainEventStoreAutoConfiguration {
}
