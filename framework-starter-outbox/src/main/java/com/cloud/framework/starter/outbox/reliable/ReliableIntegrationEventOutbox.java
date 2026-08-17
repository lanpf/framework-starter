package com.cloud.framework.starter.outbox.reliable;

import com.cloud.framework.message.integration.IntegrationEvent;
import com.cloud.framework.message.integration.IntegrationEventOutbox;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxDO;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceMapper;
import com.cloud.framework.starter.outbox.persistence.IntegrationEventOutboxPersistenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor
public class ReliableIntegrationEventOutbox implements IntegrationEventOutbox {
    private final IntegrationEventOutboxPersistenceRepository repository;
    private final IntegrationEventOutboxPersistenceMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void appendAll(List<? extends IntegrationEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        validateBatch(events);
        Instant createdAt = clock.instant();
        List<IntegrationEventOutboxDO> outboxEvents = events.stream()
                .map(event -> mapper.toDataObject(event, createdAt))
                .toList();
        String batchId = outboxEvents.get(0).getEventId();
        for (int index = 0; index < outboxEvents.size(); index++) {
            IntegrationEventOutboxDO outbox = outboxEvents.get(index);
            outbox.setBatchId(batchId);
            outbox.setBatchSequence(index);
        }
        repository.saveAll(outboxEvents);
        publishSignalAfterCommit(batchId);
    }

    private void validateBatch(List<? extends IntegrationEvent> events) {
        IntegrationEvent firstEvent = events.get(0);
        boolean sameAggregate = events.stream().allMatch(event ->
                Objects.equals(firstEvent.getAggregateType(), event.getAggregateType())
                        && Objects.equals(firstEvent.getAggregateId(), event.getAggregateId())
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
