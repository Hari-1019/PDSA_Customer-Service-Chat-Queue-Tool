package com.chatqueue.dto;

public record QueuePositionResponse(
        int vipAhead,
        int normalAhead
) {}
