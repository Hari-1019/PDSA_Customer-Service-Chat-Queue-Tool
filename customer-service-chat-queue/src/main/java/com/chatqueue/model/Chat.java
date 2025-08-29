package com.chatqueue.model;

import com.chatqueue.enums.ChatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="chats")
@Data
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="chat_id")
    private Integer chatId;

    @Column(name="customer_id")
    private UUID customerId;

    @Column(name="agent_id")
    private UUID agentId;

    @Column(name="queue_position")
    private Integer queuePosition;

    @Column(name="wait_time_minutes")
    private Integer waitTimeMinutes;

    @Column(name="started_at")
    private Instant startedAt = Instant.now();

    @Column(name="ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    private ChatStatus status = ChatStatus.waiting;

    @Column(name="customer_query")
    private String customerQuery;

    @Column(name="satisfaction_rating")
    private Integer satisfactionRating;

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public void setStatus(ChatStatus status) {
        this.status = status;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Integer getChatId() {
        return chatId;
    }
}
