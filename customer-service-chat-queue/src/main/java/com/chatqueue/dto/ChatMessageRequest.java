package com.chatqueue.dto;
import lombok.*;

@Getter @Setter
public class ChatMessageRequest {

    private Integer chatId;
    private String message;
}
