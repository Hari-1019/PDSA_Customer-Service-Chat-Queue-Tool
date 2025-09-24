package com.chatqueue.service;

import com.chatqueue.dto.*;
import com.chatqueue.model.User;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.util.JWTUtil;
import com.chatqueue.enums.UserRole;
import com.chatqueue.enums.AccountType;
import com.chatqueue.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        // Check if email already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword()); // In real app, encrypt this!
        user.setRole(UserRole.valueOf(req.getRole().toLowerCase()));
        user.setAccountType(AccountType.valueOf(req.getAccountType().toLowerCase()));
        user.setStatus(UserStatus.offline);

        if (req.getCompanyName() != null) {
            user.setCompanyName(req.getCompanyName());
        }
        if (req.getPriorityLevel() != null) {
            user.setPriorityLevel(req.getPriorityLevel());
        }//extra optional field company name priority

        userRepository.save(user);//save new user in db

        // Generate JWT token
        String token = jwtUtil.generate(user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Registration successful");

        return response;
    }

    public AuthResponse login(LoginRequest req) {
        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(req.getEmail());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOptional.get();

        // Check password (in real app, use password encoder)
        if (!user.getPassword().equals(req.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Update user status
        user.setStatus(UserStatus.available);
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generate(user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Login successful");//build and return respond

        return response;
    }

    public UserResponse getCurrentUser(String authHeader) {
        try {
            // Extract email from token
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtil.email(token);

            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("User not found");
            }

            User user = userOptional.get();

            // Convert to UserResponse
            UserResponse response = new UserResponse();
            response.setUserId(user.getUserId());
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());
            response.setAccountType(user.getAccountType());
            response.setStatus(user.getStatus());
            response.setCompanyName(user.getCompanyName());
            response.setPriorityLevel(user.getPriorityLevel());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get current user: " + e.getMessage());
        }
    }
}