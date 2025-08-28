package com.chatqueue.service;

import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.model.enums.ChatStatus;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepo;
    private final MessageRepository messageRepo;

    @Transactional
    public Chat createSession(Long customerId, Long agentId) {
        Chat c = new Chat();
        c.setCustomerId(customerId);
        c.setAgentId(agentId);
        c.setStatus(ChatStatus.IN_CHAT);
        return chatRepo.save(c);
    }

    @Transactional
    public Message appendMessage(Long sessionId, String fromRole, String content) {
        Message m = new Message();
        m.setSessionId(sessionId);
        m.setFromRole(fromRole);
        m.setContent(content);
        return messageRepo.save(m);
    }

    public List<Message> history(Long sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
