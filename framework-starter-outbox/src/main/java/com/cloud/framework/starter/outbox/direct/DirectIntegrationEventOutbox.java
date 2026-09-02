package com.cloud.framework.starter.outbox.direct;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RequiredArgsConstructor
public class DirectIntegrationEventOutbox implements IntegrationEventOutbox {
    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void appendAll(List<? extends IntegrationEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        integrationEventPublisher.publishAll(events);
    }
}
