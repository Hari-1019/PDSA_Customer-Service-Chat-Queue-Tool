package com.chatqueue.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ChatEndRequest {
    private Integer chatId;
    private UUID agentId;
    private UUID customerId;
}