package com.chatqueue.controller;

import com.chatqueue.enums.ChatStatus;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.repository.*;
import com.chatqueue.service.QueueManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminController {
    private final QueueManagerService queue;
    private final ChatRepository chats;
    private final AgentStatusRepository agentStatus;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(){
        var lengths = queue.lengths();
        var map = new LinkedHashMap<String,Object>();
        map.put("vipQueue", lengths.get("vip"));
        map.put("normalQueue", lengths.get("normal"));
        map.put("waitingChats", chats.countByStatus(ChatStatus.waiting));
        map.put("inChat", chats.countByStatus(ChatStatus.in_chat));
        map.put("closed", chats.countByStatus(ChatStatus.closed));

        List<Map<String,Object>> agents = new ArrayList<>();
        for (AgentStatusRow a : agentStatus.findAll()) {
            agents.add(Map.of(
                    "agentId", a.getAgentId(),
                    "status", a.getCurrentStatus(),
                    "currentChat", a.getCurrentChatId(),
                    "totalChatsToday", a.getTotalChatsToday()
            ));
        }
        map.put("agents", agents);
        return ResponseEntity.ok(map);
    }
}
