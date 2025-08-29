package com.chatqueue.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="queue_status")
@Getter @Setter
public class QueueStatusRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="queue_id")
    private Integer queueId;

    @Column(name="queue_type")
    private String queueType; // "enterprise_vip" or "individual_normal"

    @Column(name="customer_id")
    private UUID customerId;

    private Integer position;

    @Column(name="enqueued_at")
    private Instant enqueuedAt = Instant.now();

    @Column(name="estimated_wait_minutes")
    private Integer estimatedWaitMinutes;
}
