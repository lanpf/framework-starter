package com.cloud.framework.starter.domain.eventstore;

import com.cloud.framework.starter.domain.eventstore.store.DefaultDomainEventStoreConfiguration;
import com.cloud.framework.starter.domain.eventstore.store.NoopDomainEventStoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        DefaultDomainEventStoreConfiguration.class,
        NoopDomainEventStoreConfiguration.class
})
public class DomainEventStoreAutoConfiguration {
}
