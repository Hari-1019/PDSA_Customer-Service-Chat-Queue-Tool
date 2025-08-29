package com.chatqueue.dto;

import lombok.*;

@Getter @Setter
public class LoginRequest {
    private String email;
    private String password;
}
