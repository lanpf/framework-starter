package com.cloud.framework.starter.outbox.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class IntegrationEventOutboxDO {
    @Id
    @TableId(type = IdType.INPUT)
    private String eventId;

    private String eventType;

    private String eventClassName;

    private String batchId;

    private Integer batchSequence;

    private String aggregateType;

    private String aggregateId;

    private Instant occurredAt;

    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private Integer retryCount;

    private Instant publishedAt;

    private Instant failedAt;

    private String lastError;

    private Instant createdAt;

    private Instant updatedAt;
}
