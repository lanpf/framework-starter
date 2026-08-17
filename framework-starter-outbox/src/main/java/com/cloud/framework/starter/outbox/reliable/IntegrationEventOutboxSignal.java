package com.cloud.framework.starter.outbox.reliable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IntegrationEventOutboxSignal {
    private final String batchId;
}
