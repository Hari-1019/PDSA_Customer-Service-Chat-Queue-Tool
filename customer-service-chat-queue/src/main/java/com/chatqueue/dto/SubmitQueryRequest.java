package com.chatqueue.dto;

public record SubmitQueryRequest(
        String tempId,
        String displayName,
        String message,
        String customerType // NORMAL | VIP
) {}
