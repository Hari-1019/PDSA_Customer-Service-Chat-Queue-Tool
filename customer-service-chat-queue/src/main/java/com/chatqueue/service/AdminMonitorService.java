package com.chatqueue.service;

import com.chatqueue.model.AgentStatus;
import com.chatqueue.repository.AgentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMonitorService {

    private final SimpMessagingTemplate broker;
    private final AgentStatusRepository agentRepo;

    public void push(int vip, int normal) {
        broker.convertAndSend("/topic/admin/queue",
                new Snapshot(vip, normal, agentRepo.findAll()));
    }

    public record Snapshot(int vipSize, int normalSize, List<AgentStatus> agents) {}
}
