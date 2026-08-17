package com.cloud.framework.starter.outbox.publisher;

import com.cloud.framework.message.PartitionedOperations;
import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventHeaders;
import com.cloud.framework.message.integration.IntegrationEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public class PartitionedIntegrationEventPublisher implements IntegrationEventPublisher {
    private final PartitionedOperations partitionedOperations;

    @Override
    public void publish(IntegrationEvent event) {
        partitionedOperations.convertAndSend(
                event.getAggregateType(),
                event,
                IntegrationEventHeaders::from,
                IntegrationEvent::getAggregateId
        );
    }

    @Override
    public void publishAll(List<? extends IntegrationEvent> events) {
        partitionedOperations.convertAndSendBatch(
                resolveDestination(events),
                events,
                IntegrationEventHeaders::from,
                IntegrationEvent::getAggregateId
        );
    }

    private String resolveDestination(List<? extends IntegrationEvent> events) {
        String destination = events.get(0).getAggregateType();
        boolean sameDestination = events.stream()
                .allMatch(event -> ObjectUtils.nullSafeEquals(destination, event.getAggregateType()));
        if (!sameDestination) {
            throw new IllegalArgumentException("Integration event batch must use one aggregate type.");
        }
        return destination;
    }
}
