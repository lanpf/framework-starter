package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence;

import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;

public interface IntegrationEventOutboxJpaPersistenceMapper {

    IntegrationEventOutboxDO toDataObject(IntegrationEventOutboxEnvelope envelope);

    IntegrationEventOutboxEnvelope toEnvelope(IntegrationEventOutboxDO outbox);
}
