package com.chatqueue.model;

import com.chatqueue.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="agent_status")
@Getter @Setter
public class AgentStatusRow {

    @Id
    @Column(name="agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name="current_status")
    private UserStatus currentStatus = UserStatus.offline;

    @Column(name="current_chat_id")
    private Integer currentChatId;

    @Column(name="last_activity")
    private Instant lastActivity = Instant.now();

    @Column(name="total_chats_today")
    private Integer totalChatsToday = 0;
}