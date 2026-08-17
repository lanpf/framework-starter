package com.cloud.framework.starter.outbox.publisher;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopIntegrationEventPublisher implements IntegrationEventPublisher {
    @Override
    public void publish(IntegrationEvent event) {
        log.info(
                "Noop integration event publisher ignored event. eventId={}, eventType={}, aggregateId={}, aggregateType={}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getAggregateType()
        );
    }
}
