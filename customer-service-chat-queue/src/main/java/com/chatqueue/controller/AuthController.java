package com.chatqueue.controller;

import com.chatqueue.dto.AuthResponse;
import com.chatqueue.dto.LoginRequest;
import com.chatqueue.dto.RegisterRequest;
import com.chatqueue.model.User;
import com.chatqueue.model.enums.AccountType;
import com.chatqueue.model.enums.UserRole;
import com.chatqueue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email());
        u.setPasswordHash(req.password());  // demo only; hash in real app
        u.setRole(UserRole.CUSTOMER);
        u.setAccountType(AccountType.INDIVIDUAL);
        return userService.register(u);
    }
}
