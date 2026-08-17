package com.cloud.framework.starter.outbox.reliable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.RetryOperations;

@Slf4j
@RequiredArgsConstructor
public class IntegrationEventOutboxSignalListener {
    private final IntegrationEventOutboxPublisher outboxPublisher;
    private final AsyncTaskExecutor taskExecutor;
    private final RetryOperations retryOperations;

    @EventListener
    public void on(IntegrationEventOutboxSignal signal) {
        taskExecutor.execute(() -> publish(signal));
    }

    private void publish(IntegrationEventOutboxSignal signal) {
        try {
            retryOperations.execute(context -> {
                outboxPublisher.publish(signal.getBatchId());
                return null;
            });
        } catch (Exception ex) {
            log.error("Integration event outbox signal exhausted retries. batchId={}", signal.getBatchId(), ex);
        }
    }
}
