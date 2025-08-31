package com.chatqueue.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String messageText;
    // Remove chatId since we'll determine it on the server
}