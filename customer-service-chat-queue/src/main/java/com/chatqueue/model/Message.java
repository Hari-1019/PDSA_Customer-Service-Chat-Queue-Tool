package com.chatqueue.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;   // Chat.id

    private String fromRole;  // "CX" or "AGENT"

    @Column(length = 4000)
    private String content;

    private Instant createdAt = Instant.now();
}
