package com.chatqueue.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor

public class AuthResponse {
    private String email;
    private String role;
    private String token;
}
