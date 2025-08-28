package com.chatqueue.model;

import com.chatqueue.model.enums.ChatStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Chat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // sessionId
    private Long customerId;      // optional link to User (CUSTOMER)
    private Long agentId;         // link to AgentStatus.id

    @Enumerated(EnumType.STRING)
    private ChatStatus status = ChatStatus.WAITING;

    private Instant createdAt = Instant.now();
}
