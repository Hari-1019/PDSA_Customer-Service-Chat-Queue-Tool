package com.chatqueue.service;

import com.chatqueue.enums.ChatStatus;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.model.QueueStatusRow;
import com.chatqueue.repository.AgentStatusRepository;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import com.chatqueue.repository.QueueStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepo;
    private final MessageRepository messageRepo;
    private final AgentStatusRepository agentRepo;
    private final QueueStatusRepository queueStatusRepository;

    public Optional<Chat> assignNextToAgent(UUID agentId) {
        try {
            // Get next customer from queue (VIP first, then normal)
            List<QueueStatusRow> vipQueue = queueStatusRepository.findByQueueTypeOrderByPositionAsc("enterprise_vip");
            List<QueueStatusRow> normalQueue = queueStatusRepository.findByQueueTypeOrderByPositionAsc("individual_normal");

            Optional<QueueStatusRow> nextCustomer = Optional.empty();

            if (!vipQueue.isEmpty()) {
                nextCustomer = Optional.of(vipQueue.get(0));
            } else if (!normalQueue.isEmpty()) {
                nextCustomer = Optional.of(normalQueue.get(0));
            }

            if (nextCustomer.isEmpty()) {
                return Optional.empty();
            }

            UUID customerId = nextCustomer.get().getCustomerId();
            var chat = chatRepo.findByCustomerIdAndStatus(customerId, ChatStatus.waiting)
                    .orElseThrow(() -> new RuntimeException("No waiting chat found for customer: " + customerId));

            // Assign the chat to the agent and update its status
            chat.setAgentId(agentId);
            chat.setStatus(ChatStatus.in_chat);
            chat.setStartedAt(Instant.now());
            chatRepo.save(chat);

            // Update the agent's status in agent_status table
            var agentStatus = agentRepo.findById(agentId).orElseGet(() -> {
                var newAgentStatus = new AgentStatusRow();
                newAgentStatus.setAgentId(agentId);
                return newAgentStatus;
            });
            agentStatus.setCurrentStatus(UserStatus.busy);
            agentStatus.setCurrentChatId(chat.getChatId());
            agentStatus.setLastActivity(Instant.now());
            agentRepo.save(agentStatus);

            // Remove the customer from the queue - FIXED
            queueStatusRepository.deleteById(Math.toIntExact(nextCustomer.get().getQueueId()));

            return Optional.of(chat);
        } catch (Exception e) {
            throw new RuntimeException("Error assigning next customer: " + e.getMessage());
        }
    }

    /**
     * Send a message in a chat session
     */
    public Message send(Integer chatId, UUID senderId, String text) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        if (chat.getStatus() != ChatStatus.in_chat) {
            throw new RuntimeException("Chat is not active");
        }

        var message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setMessageText(text);
        message.setTimestamp(Instant.now());
        return messageRepo.save(message);
    }

    /**
     * Get chat history for a given chat session
     */
    public List<Message> history(Integer chatId) {
        return messageRepo.findByChatIdOrderByTimestampAsc(chatId);
    }

    /**
     * Close a chat session and update agent status
     */
    public void close(Integer chatId, UUID agentId) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        if (!agentId.equals(chat.getAgentId())) {
            throw new RuntimeException("Agent is not assigned to this chat");
        }

        chat.setStatus(ChatStatus.closed);
        chat.setEndedAt(Instant.now());
        chatRepo.save(chat);

        var agentStatus = agentRepo.findById(agentId).orElseThrow(() -> new RuntimeException("Agent not found"));
        agentStatus.setCurrentStatus(UserStatus.available);
        agentStatus.setCurrentChatId(null);
        agentStatus.setLastActivity(Instant.now());
        agentRepo.save(agentStatus);
    }
    // Add this method to ChatService
    public void notifyChatEnded(Integer chatId, String message) {
        // This will be used by WebSocket to notify users
    }
}