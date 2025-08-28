package com.chatqueue.controller;

import com.chatqueue.dto.AdminDashboardResponse;
import com.chatqueue.repository.AgentStatusRepository;
import com.chatqueue.service.QueueManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final QueueManagerService queueService;
    private final AgentStatusRepository agentRepo;

    @GetMapping("/health")
    public String health(){ return "up"; }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                queueService.vipSize(),
                queueService.normalSize(),
                agentRepo.findAll()
        );
    }
}
