package com.chatqueue.model;

import com.chatqueue.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;

@Entity
@Getter @Setter @NoArgsConstructor
public class AgentStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agentName;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.AVAILABLE;

    private int chatCount = 0;
}
