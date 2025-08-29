package com.chatqueue.service;

import com.chatqueue.dto.*;
import com.chatqueue.enums.*;
import com.chatqueue.enums.UserRole;
import com.chatqueue.model.*;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.chatqueue.exception.EmailAlreadyExistsException;

@Service
@RequiredArgsConstructor

public class UserService {


    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JWTUtil jwt;

    public AuthResponse register(RegisterRequest req) {
        users.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new EmailAlreadyExistsException("Email already registered");
        });
        var u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole(UserRole.valueOf(req.getRole()));
        u.setAccountType(AccountType.valueOf(req.getAccountType()));
        u.setCompanyName(req.getCompanyName());
        u.setPriorityLevel("enterprise".equals(req.getAccountType()) ? 2 : 1);
        u.setStatus(UserStatus.available);
        users.save(u);
        var token = jwt.generate(u.getEmail(), u.getRole().name());
        return new AuthResponse(u.getEmail(), u.getRole().name(), token);
    }

    public AuthResponse login(LoginRequest req) {
        var u = users.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!encoder.matches(req.getPassword(), u.getPassword()))
            throw new RuntimeException("Bad credentials");
        var token = jwt.generate(u.getEmail(), u.getRole().name());
        return new AuthResponse(u.getEmail(), u.getRole().name(), token);
    }
}

