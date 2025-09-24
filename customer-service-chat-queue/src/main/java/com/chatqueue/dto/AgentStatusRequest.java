package com.chatqueue.dto;

import lombok.Data;

@Data
public class AgentStatusRequest {
    private String status; // This should be a string like "available", "busy", etc.
}