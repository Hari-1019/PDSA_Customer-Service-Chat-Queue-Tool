package com.chatqueue.service;

import com.chatqueue.dto.ChatAssignmentResponse;
import com.chatqueue.dto.QueuePositionResponse;
import com.chatqueue.model.AgentStatus;
import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.model.enums.UserStatus;
import com.chatqueue.repository.AgentStatusRepository;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import com.chatqueue.util.QueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class QueueManagerService {

    private final AgentStatusRepository agentRepo;
    private final ChatRepository chatRepo;
    private final MessageRepository msgRepo;
    private final SimpMessagingTemplate broker;
    private final AdminMonitorService admin;

    private final QueueManager<Ticket> qVip = new QueueManager<>();
    private final QueueManager<Ticket> qNormal = new QueueManager<>();

    // quick lookup by tempId → which queue & position support
    private final Map<String, Ticket> ticketIndex = new ConcurrentHashMap<>();

    public record Ticket(String tempId, String name, String message, String type, Instant createdAt) {}

    // ========== Public API ==========

    @Transactional
    public void enqueue(String tempId, String name, String message, String typeRaw) {
        String type = (typeRaw != null && typeRaw.equalsIgnoreCase("VIP")) ? "VIP" : "NORMAL";
        Ticket t = new Ticket(tempId, name, message, type, Instant.now());
        ticketIndex.put(tempId, t);
        if ("VIP".equals(type)) qVip.add(t); else qNormal.add(t);

        pushAdmin();
        tryAssignAnyFreeAgent();
    }

    @Transactional
    public void setAgentAvailability(Long agentId, boolean available) {
        AgentStatus a = agentRepo.findById(agentId).orElseThrow();
        a.setStatus(available ? UserStatus.AVAILABLE : UserStatus.BUSY);
        agentRepo.save(a);
        pushAdmin();
        if (available) tryAssign(agentId);
    }

    @Transactional
    public void receiveChat(Long sessionId, String fromRole, String content) {
        Message m = new Message();
        m.setSessionId(sessionId);
        m.setFromRole(fromRole);
        m.setContent(content);
        msgRepo.save(m);

        broker.convertAndSend("/topic/chat." + sessionId, m);
    }

    @Transactional
    public void closeChat(Long sessionId) {
        Chat s = chatRepo.findById(sessionId).orElseThrow();
        // mark closed
        // (we keep ChatStatus in ChatService, but not imported here to keep deps light)
        // If you want: s.setStatus(ChatStatus.CLOSED);
        chatRepo.save(s);

        AgentStatus a = agentRepo.findById(s.getAgentId()).orElseThrow();
        a.setStatus(UserStatus.AVAILABLE);
        a.setChatCount(a.getChatCount() + 1);
        agentRepo.save(a);

        pushAdmin();
        tryAssign(a.getId());
    }

    public QueuePositionResponse positionOf(String tempId, String typeRaw) {
        String type = (typeRaw != null && typeRaw.equalsIgnoreCase("VIP")) ? "VIP" : "NORMAL";
        int vipAhead = qVip.indexOf(tempId);
        int normalAhead = qNormal.indexOf(tempId);
        if ("VIP".equals(type)) {
            // In VIP queue: show VIP ahead as index; normal ahead is full normal size (served after VIPs)
            return new QueuePositionResponse(Math.max(vipAhead, 0), qNormal.size());
        } else {
            // In Normal queue: all VIPs are ahead + index in normal
            int normalIndex = Math.max(normalAhead, 0);
            return new QueuePositionResponse(qVip.size(), normalIndex);
        }
    }

    public int vipSize() { return qVip.size(); }
    public int normalSize() { return qNormal.size(); }

    // ========== Internal helpers ==========

    private synchronized void tryAssignAnyFreeAgent() {
        agentRepo.findByStatus(UserStatus.AVAILABLE).forEach(a -> tryAssign(a.getId()));
    }

    private synchronized void tryAssign(Long agentId) {
        AgentStatus agent = agentRepo.findById(agentId).orElseThrow();
        if (agent.getStatus() != UserStatus.AVAILABLE) return;

        // VIP first
        Ticket t = qVip.poll();
        if (t == null) t = qNormal.poll();
        if (t == null) return;

        // remove index
        ticketIndex.remove(t.tempId());

        // mark agent busy
        agent.setStatus(UserStatus.BUSY);
        agentRepo.save(agent);

        // create session
        Chat session = new Chat();
        session.setAgentId(agent.getId());
        // optional: link to a persisted user in future; for now keep customerId null
        session = chatRepo.save(session);

        // notify agent and customer
        ChatAssignmentResponse payload = new ChatAssignmentResponse(
                session.getId(),
                agent.getId(),
                t.name(),
                t.message(),
                t.type()
        );

        broker.convertAndSend("/topic/agent." + agent.getId(), payload);
        broker.convertAndSend("/topic/cx." + t.tempId(), payload);

        pushAdmin();
    }

    private void pushAdmin() {
        admin.push(qVip.size(), qNormal.size());
    }
}
