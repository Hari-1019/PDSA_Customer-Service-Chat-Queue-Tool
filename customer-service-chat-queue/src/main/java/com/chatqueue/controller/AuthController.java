package com.chatqueue.controller;

import com.chatqueue.dto.*;
import com.chatqueue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService users;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req){
        return ResponseEntity.ok(users.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req){
        return ResponseEntity.ok(users.login(req));
    }
}
