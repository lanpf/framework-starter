package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelope;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxEnvelopePersistenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@RequiredArgsConstructor
public class ReliableIntegrationEventOutbox implements IntegrationEventOutbox {
    private final IntegrationEventOutboxEnvelopePersistenceRepository repository;
    private final IntegrationEventOutboxEnvelopePersistenceMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void appendAll(List<? extends IntegrationEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        validateBatch(events);
        Instant createdAt = clock.instant();
        String batchId = events.get(0).getEventId();
        List<IntegrationEventOutboxEnvelope> envelopes = new ArrayList<>(events.size());
        for (int index = 0; index < events.size(); index++) {
            envelopes.add(this.mapper.toEnvelope(events.get(index), createdAt, batchId, index));
        }
        repository.saveAll(envelopes);
        publishSignalAfterCommit(batchId);
    }

    private void validateBatch(List<? extends IntegrationEvent> events) {
        IntegrationEvent firstEvent = events.get(0);
        boolean sameAggregate = events.stream().allMatch(event ->
                ObjectUtils.nullSafeEquals(firstEvent.getAggregateType(), event.getAggregateType())
                        && ObjectUtils.nullSafeEquals(firstEvent.getAggregateId(), event.getAggregateId())
        );
        if (!sameAggregate) {
            throw new IllegalArgumentException("Integration event outbox batch must belong to one aggregate.");
        }
    }

    private void publishSignalAfterCommit(String batchId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(new IntegrationEventOutboxSignal(batchId));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new IntegrationEventOutboxSignal(batchId));
            }
        });
    }
}
