package com.chatqueue.service;

import com.chatqueue.enums.ChatStatus;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.repository.AgentStatusRepository;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepo;
    private final MessageRepository messageRepo;
    private final AgentStatusRepository agentRepo;
    private final QueueManagerService queueManager;

    public Optional<Chat> assignNextToAgent(UUID agentId) {
        var nextQueue = queueManager.peekNext();
        if (nextQueue.isEmpty()) return Optional.empty();

        var customerId = nextQueue.get().getCustomerId();
        var chat = chatRepo.findByCustomerIdAndStatus(customerId, ChatStatus.waiting) // lowercase
                .orElseThrow(() -> new RuntimeException("No waiting chat found"));

        // Assign the chat to the agent and update its status
        chat.setAgentId(agentId);
        chat.setStatus(ChatStatus.in_chat); // lowercase
        chat.setStartedAt(Instant.now());
        chatRepo.save(chat);

        // Update the agent's status
        var agentStatus = agentRepo.findById(agentId).orElseGet(() -> {
            var agent = new AgentStatusRow();
            agent.setAgentId(agentId);
            return agent;
        });
        agentStatus.setCurrentStatus(UserStatus.busy); // lowercase
        agentStatus.setCurrentChatId(chat.getChatId());
        agentRepo.save(agentStatus);

        // Remove the customer from the queue
        queueManager.removeFromQueue(customerId);

        return Optional.of(chat);
    }

    public Message send(Integer chatId, UUID senderId, String text) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        if (chat.getStatus() != ChatStatus.in_chat) { // lowercase
            throw new RuntimeException("Chat is not active");
        }

        var message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setMessageText(text);
        message.setTimestamp(Instant.now());
        return messageRepo.save(message);
    }

    public List<Message> history(Integer chatId) {
        return messageRepo.findByChatIdOrderByTimestampAsc(chatId);
    }

    public void close(Integer chatId, UUID agentId) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        if (!agentId.equals(chat.getAgentId())) {
            throw new RuntimeException("Agent is not assigned to this chat");
        }

        chat.setStatus(ChatStatus.closed); // lowercase
        chat.setEndedAt(Instant.now());
        chatRepo.save(chat);

        var agentStatus = agentRepo.findById(agentId).orElseThrow(() -> new RuntimeException("Agent not found"));
        agentStatus.setCurrentStatus(UserStatus.available); // lowercase
        agentStatus.setCurrentChatId(null);
        agentRepo.save(agentStatus);
    }
}