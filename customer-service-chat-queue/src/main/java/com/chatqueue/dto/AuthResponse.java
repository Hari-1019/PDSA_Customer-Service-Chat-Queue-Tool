package com.chatqueue.dto;

import com.chatqueue.model.enums.UserRole;

public record AuthResponse(
        Long userId,
        String token,
        UserRole role
) {}
