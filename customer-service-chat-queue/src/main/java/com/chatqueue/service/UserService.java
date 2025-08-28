package com.chatqueue.service;

import com.chatqueue.dto.AuthResponse;
import com.chatqueue.dto.LoginRequest;
import com.chatqueue.model.User;
import com.chatqueue.model.enums.UserRole;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    // demo login
    public AuthResponse login(LoginRequest req) {
        // In production: verify hash in DB. Here we return a fake token.
        return new AuthResponse(1L, "demo-token", UserRole.CUSTOMER);
    }

    public AuthResponse register(User u) {
        // In production: persist user and hash password.
        return new AuthResponse(2L, "demo-register-token", u.getRole());
    }
}
