package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.persistence;

import com.cloud.framework.starter.outbox.persistence.OutboxStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationEventOutboxJpaRepository extends JpaRepository<IntegrationEventOutboxDO, String> {
    List<IntegrationEventOutboxDO> findByBatchIdOrderByBatchSequenceAsc(String batchId);

    @Query("""
            select outbox.batchId
              from IntegrationEventOutboxDO outbox
             where outbox.status = :status
               and outbox.retryCount < :retryCount
             group by outbox.batchId
             order by min(outbox.createdAt)
            """)
    List<String> findBatchIdsByStatusAndRetryCountLessThan(
            @Param("status") OutboxStatus status,
            @Param("retryCount") Integer retryCount,
            Pageable pageable
    );

    @Query("""
            select outbox.batchId
              from IntegrationEventOutboxDO outbox
             where outbox.status = :status
               and outbox.updatedAt < :updatedBefore
               and outbox.retryCount < :retryCount
             group by outbox.batchId
             order by min(outbox.createdAt)
            """)
    List<String> findBatchIdsByStatusAndUpdatedBeforeAndRetryCountLessThan(
            @Param("status") OutboxStatus status,
            @Param("updatedBefore") Instant updatedBefore,
            @Param("retryCount") Integer retryCount,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update IntegrationEventOutboxDO outbox
               set outbox.status = :publishingStatus,
                   outbox.updatedAt = :publishingAt
             where outbox.batchId = :batchId
               and outbox.status = :pendingStatus
            """)
    Integer markPublishingByBatchId(
            @Param("batchId") String batchId,
            @Param("publishingAt") Instant publishingAt,
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("publishingStatus") OutboxStatus publishingStatus
    );

    @Modifying
    @Query("""
            update IntegrationEventOutboxDO outbox
               set outbox.status = :publishedStatus,
                   outbox.publishedAt = :publishedAt,
                   outbox.updatedAt = :publishedAt
             where outbox.batchId = :batchId
               and outbox.status = :publishingStatus
            """)
    Integer markPublishedByBatchId(
            @Param("batchId") String batchId,
            @Param("publishedAt") Instant publishedAt,
            @Param("publishingStatus") OutboxStatus publishingStatus,
            @Param("publishedStatus") OutboxStatus publishedStatus
    );

    @Modifying
    @Query("""
            update IntegrationEventOutboxDO outbox
               set outbox.status = :pendingStatus,
                   outbox.updatedAt = :pendingAt
             where outbox.batchId = :batchId
               and outbox.status = :publishingStatus
               and outbox.updatedAt = :publishingAt
            """)
    Integer restorePendingByBatchId(
            @Param("batchId") String batchId,
            @Param("publishingAt") Instant publishingAt,
            @Param("pendingAt") Instant pendingAt,
            @Param("publishingStatus") OutboxStatus publishingStatus,
            @Param("pendingStatus") OutboxStatus pendingStatus
    );

    @Modifying
    @Query("""
            update IntegrationEventOutboxDO outbox
               set outbox.status = :pendingStatus,
                   outbox.updatedAt = :pendingAt
             where outbox.batchId = :batchId
               and outbox.status = :publishingStatus
               and outbox.updatedAt < :publishingBefore
            """)
    Integer restoreExpiredPublishingByBatchId(
            @Param("batchId") String batchId,
            @Param("publishingBefore") Instant publishingBefore,
            @Param("pendingAt") Instant pendingAt,
            @Param("publishingStatus") OutboxStatus publishingStatus,
            @Param("pendingStatus") OutboxStatus pendingStatus
    );
}
