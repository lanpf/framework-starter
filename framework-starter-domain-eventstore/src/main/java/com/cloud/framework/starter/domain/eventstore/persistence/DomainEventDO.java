package com.cloud.framework.starter.domain.eventstore.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class DomainEventDO {
    @Id
    @TableId(type = IdType.INPUT)
    private Long eventId;

    private String eventType;

    private Instant occurredAt;

    private String payload;
}
