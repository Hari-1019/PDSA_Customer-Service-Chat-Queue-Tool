package com.chatqueue.dto;

public record ChatMessageRequest(
        Long sessionId,
        String fromRole,   // "CX" or "AGENT"
        String content
) {}
