package com.cloud.framework.starter.outbox.persistence;

import com.cloud.framework.message.integration.IntegrationEvent;
import java.time.Instant;

public interface IntegrationEventOutboxPersistenceMapper {
    IntegrationEventOutboxDO toDataObject(IntegrationEvent event, Instant createdAt);

    IntegrationEvent toIntegrationEvent(IntegrationEventOutboxDO outbox);
}
