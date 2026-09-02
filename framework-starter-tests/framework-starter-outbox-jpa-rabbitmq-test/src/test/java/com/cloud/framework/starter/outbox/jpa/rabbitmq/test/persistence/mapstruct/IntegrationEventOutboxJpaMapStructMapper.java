package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.mapstruct;

import com.cloud.framework.core.mapper.MapStructConfig;
import com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence.IntegrationEventOutboxJpaPersistenceMapper;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface IntegrationEventOutboxJpaMapStructMapper extends IntegrationEventOutboxJpaPersistenceMapper {
}
