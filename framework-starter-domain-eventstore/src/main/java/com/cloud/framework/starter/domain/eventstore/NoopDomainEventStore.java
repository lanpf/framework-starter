package com.cloud.framework.starter.domain.eventstore;

import com.cloud.framework.domain.DomainEvent;
import com.cloud.framework.domain.DomainEventStore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopDomainEventStore implements DomainEventStore {
    @Override
    public void appendAll(List<? extends DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            log.info("Noop domain event store ignored empty domain events.");
            return;
        }
        domainEvents.forEach(domainEvent -> log.info(
                "Noop domain event store ignored event. eventType={}",
                domainEvent.getEventType()
        ));
    }
}
