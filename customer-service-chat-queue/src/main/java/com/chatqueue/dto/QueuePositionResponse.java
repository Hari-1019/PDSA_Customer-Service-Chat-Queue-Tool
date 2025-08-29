package com.chatqueue.dto;

import lombok.*;
@Getter @Setter @AllArgsConstructor

public class QueuePositionResponse {
    private Integer chatId;
    private Integer position;
}
