package com.chatqueue.dto;

import com.chatqueue.enums.AccountType;
import com.chatqueue.enums.UserRole;
import com.chatqueue.enums.UserStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResponse {
    private UUID userId;
    private String name;
    private String email;
    private UserRole role;
    private AccountType accountType;
    private UserStatus status;
    private String companyName;
    private Integer priorityLevel;
}