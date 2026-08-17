package com.cloud.framework.starter.outbox.persistence;

public enum OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED
}
