package com.chatqueue.controller;

import com.chatqueue.dto.QueuePositionResponse;
import com.chatqueue.service.QueueManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final QueueManagerService queueService;

    // Query position by tempId (generated in browser) and type (NORMAL/VIP)
    @GetMapping("/position")
    public QueuePositionResponse position(
            @RequestParam String tempId,
            @RequestParam(defaultValue = "NORMAL") String type
    ) {
        return queueService.positionOf(tempId, type);
    }
}
