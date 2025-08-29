package com.chatqueue.controller;

import com.chatqueue.dto.*;
import com.chatqueue.model.*;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.service.QueueManagerService;
import com.chatqueue.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController @RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final QueueManagerService queue;
    private final UserRepository users;
    private final JWTUtil jwt;

    // 1) Customer submits a query -> enqueued
    @PostMapping("/submit")
    public ResponseEntity<QueuePositionResponse> submit(@RequestHeader("Authorization") String auth,
                                                        @RequestBody SubmitQueryRequest body){
        var email = jwt.email(auth.substring(7));
        var user = users.findByEmail(email).orElseThrow();
        var resp = queue.enqueue(user.getUserId(), body.getCustomerQuery());
        return ResponseEntity.ok(resp);
    }

    // 2) Check lengths (for CX UI showing "You are #n")
    @GetMapping("/queues")
    public ResponseEntity<?> getQueues() {
        return ResponseEntity.ok(queue.lengths());
    }
}
