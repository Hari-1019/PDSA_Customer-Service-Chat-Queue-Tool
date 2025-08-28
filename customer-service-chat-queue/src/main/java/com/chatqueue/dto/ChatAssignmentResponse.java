package com.chatqueue.dto;

public record ChatAssignmentResponse(
        Long sessionId,
        Long agentId,
        String customerDisplayName,
        String customerMessage,
        String customerType
) {}
