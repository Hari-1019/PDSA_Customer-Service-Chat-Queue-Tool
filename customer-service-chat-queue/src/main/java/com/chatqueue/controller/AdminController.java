package com.chatqueue.controller;

import com.chatqueue.enums.ChatStatus;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.model.User;
import com.chatqueue.repository.*;
import com.chatqueue.service.QueueManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final QueueManagerService queueManagerService;
    private final ChatRepository chatRepository;
    private final AgentStatusRepository agentStatusRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        try {
            // DEBUG: Check what's in the database
            System.out.println("=== ADMIN DASHBOARD CALLED ===");

            // Use the same method as AgentController for consistency
            long vipCount = queueManagerService.getQueueCount("enterprise_vip");
            long normalCount = queueManagerService.getQueueCount("individual_normal");

            System.out.println("VIP count: " + vipCount + ", Normal count: " + normalCount);

            // Get chat counts
            long waitingChats = chatRepository.countByStatus(ChatStatus.waiting);
            long inChat = chatRepository.countByStatus(ChatStatus.in_chat);
            long closed = chatRepository.countByStatus(ChatStatus.closed);

            Map<String, Object> dashboardData = new LinkedHashMap<>();
            dashboardData.put("vipQueue", vipCount);
            dashboardData.put("normalQueue", normalCount);
            dashboardData.put("waitingChats", waitingChats);
            dashboardData.put("inChat", inChat);
            dashboardData.put("closed", closed);

            // Get all agents with their status
            List<Map<String, Object>> agents = new ArrayList<>();
            List<AgentStatusRow> agentStatuses = agentStatusRepository.findAll();

            for (AgentStatusRow agent : agentStatuses) {
                Optional<User> userOptional = userRepository.findById(agent.getAgentId());
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    // Use HashMap instead of Map.of() to avoid NullPointerException
                    Map<String, Object> agentInfo = new HashMap<>();
                    agentInfo.put("agentId", agent.getAgentId());
                    agentInfo.put("name", user.getName());
                    agentInfo.put("status", agent.getCurrentStatus() != null ? agent.getCurrentStatus().toString() : "offline");
                    agentInfo.put("currentChat", agent.getCurrentChatId());
                    agentInfo.put("totalChatsToday", agent.getTotalChatsToday() != null ? agent.getTotalChatsToday() : 0);
                    agentInfo.put("lastActivity", agent.getLastActivity());

                    agents.add(agentInfo);
                }
            }

            dashboardData.put("agents", agents);

            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            System.out.println("ERROR in admin dashboard: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error fetching dashboard data: " + e.getMessage());
        }
    }

    @GetMapping("/queue-status")
    public ResponseEntity<?> getQueueStatus() {
        try {
            long vipCount = queueManagerService.getQueueCount("enterprise_vip");
            long normalCount = queueManagerService.getQueueCount("individual_normal");

            // Use HashMap instead of Map.of() to avoid potential issues
            Map<String, Object> queueData = new HashMap<>();
            queueData.put("vipCount", vipCount);
            queueData.put("normalCount", normalCount);

            return ResponseEntity.ok(queueData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching queue status: " + e.getMessage());
        }
    }

    @GetMapping("/agents")
    public ResponseEntity<?> getAgentStatus() {
        try {
            List<AgentStatusRow> agentStatuses = agentStatusRepository.findAll();
            List<Map<String, Object>> agents = new ArrayList<>();

            for (AgentStatusRow agentStatus : agentStatuses) {
                Optional<User> userOptional = userRepository.findById(agentStatus.getAgentId());
                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    Map<String, Object> agentInfo = new HashMap<>();
                    agentInfo.put("agentId", agentStatus.getAgentId());
                    agentInfo.put("name", user.getName());
                    agentInfo.put("email", user.getEmail());
                    agentInfo.put("status", agentStatus.getCurrentStatus());
                    agentInfo.put("currentChatId", agentStatus.getCurrentChatId());
                    agentInfo.put("totalChatsToday", agentStatus.getTotalChatsToday());
                    agentInfo.put("lastActivity", agentStatus.getLastActivity());

                    agents.add(agentInfo);
                }
            }

            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching agent status: " + e.getMessage());
        }
    }
}