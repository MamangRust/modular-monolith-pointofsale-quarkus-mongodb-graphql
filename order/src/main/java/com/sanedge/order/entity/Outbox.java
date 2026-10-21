package com.sanedge.order.entity;

import java.time.Instant;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@MongoEntity(collection = "order_outbox")
public class Outbox extends ReactivePanacheMongoEntity {

    private String aggregateType;

    private String aggregateId;

    private String topic;

    private String payload;

    private String domain;

    private String eventId;

    private OutboxStatus status = OutboxStatus.PENDING;

    private Integer attempts = 0;

    private Instant createdAt;

    private Instant processedAt;

    private String lastError;

    private Instant nextAttemptAt;

    private Instant claimedAt;

    private String claimToken;

    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
