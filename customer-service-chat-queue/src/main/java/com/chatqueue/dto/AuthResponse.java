package com.chatqueue.dto;

import lombok.*;

@Getter @Setter

public class AuthResponse {
    private String email;
    private String role;
    private String token;

    public AuthResponse(String email, String role, String token) {
        this.email = email;
        this.role = role;
        this.token = token;
    }

}
