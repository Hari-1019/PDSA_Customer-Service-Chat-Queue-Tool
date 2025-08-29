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
    private final ChatRepository chatRepo; // Repository for managing chat entities in the database
    private final MessageRepository messageRepo; // Repository for managing message entities in the database
    private final AgentStatusRepository agentRepo; // Repository for managing agent status entities in the database
    private final QueueManagerService queueManager; // Service for managing the customer queue

    /**
     * Assigns the next customer in the queue to an available agent.
     * - Retrieves the next customer from the queue.
     * - Finds the customer's waiting chat session.
     * - Updates the chat session to assign it to the agent and mark it as "in_chat".
     * - Updates the agent's status to "busy" and links the agent to the chat.
     * - Removes the customer from the queue.
     */
    public Optional<Chat> assignNextToAgent(UUID agentId) {
        var nextQueue = queueManager.peekNext(); // Get the next customer in the queue
        if (nextQueue.isEmpty()) return Optional.empty(); // If no customer is in the queue, return empty

        var customerId = nextQueue.get().getCustomerId(); // Get the customer ID from the queue
        var chat = chatRepo.findByCustomerIdAndStatus(customerId, ChatStatus.waiting)
                .orElseThrow(() -> new RuntimeException("No waiting chat found")); // Find the waiting chat for the customer

        // Assign the chat to the agent and update its status
        chat.setAgentId(agentId);
        chat.setStatus(ChatStatus.in_chat);
        chat.setStartedAt(Instant.now());
        chatRepo.save(chat);

        // Update the agent's status to "busy" and link the agent to the chat
        var agentStatus = agentRepo.findById(agentId).orElseGet(() -> {
            var agent = new AgentStatusRow();
            agent.setAgentId(agentId);
            return agent;
        });
        agentStatus.setCurrentStatus(UserStatus.busy);
        agentStatus.setCurrentChatId(chat.getChatId());
        agentRepo.save(agentStatus);

        // Remove the customer from the queue
        queueManager.removeFromQueue(customerId);

        return Optional.of(chat); // Return the assigned chat
    }

    /**
     * Sends a message in a chat session.
     * - Validates if the chat exists and is active.
     * - Creates a new message entity.
     * - Sets the chat ID, sender ID, and message text.
     * - Saves the message to the database.
     */
    public Message send(Integer chatId, UUID senderId, String text) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found")); // Validate chat existence
        if (chat.getStatus() != ChatStatus.in_chat) {
            throw new RuntimeException("Chat is not active"); // Ensure the chat is active
        }

        var message = new Message(); // Create a new message object
        message.setChatId(chatId); // Set the chat ID
        message.setSenderId(senderId); // Set the sender ID
        message.setMessageText(text); // Set the message text
        message.setTimestamp(Instant.now()); // Set the timestamp
        return messageRepo.save(message); // Save the message to the database and return it
    }

    /**
     * Retrieves the chat history for a given chat session.
     * - Fetches all messages for the specified chat ID.
     * - Orders the messages by their timestamp in ascending order.
     */
    public List<Message> history(Integer chatId) {
        return messageRepo.findByChatIdOrderByTimestampAsc(chatId); // Retrieve and return the chat history
    }

    /**
     * Closes a chat session and updates the agent's status to available.
     * - Validates if the chat exists and the agent is assigned to it.
     * - Updates the chat status to "closed" and sets the end timestamp.
     * - Updates the agent's status to "available" and clears the current chat ID.
     */
    public void close(Integer chatId, UUID agentId) {
        var chat = chatRepo.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found")); // Validate chat existence
        if (!agentId.equals(chat.getAgentId())) {
            throw new RuntimeException("Agent is not assigned to this chat"); // Ensure the agent is assigned to the chat
        }

        chat.setStatus(ChatStatus.closed); // Set the chat status to "closed"
        chat.setEndedAt(Instant.now()); // Set the end timestamp
        chatRepo.save(chat); // Save the updated chat

        var agentStatus = agentRepo.findById(agentId).orElseThrow(() -> new RuntimeException("Agent not found")); // Validate agent existence
        agentStatus.setCurrentStatus(UserStatus.available); // Set the agent's status to "available"
        agentStatus.setCurrentChatId(null); // Clear the current chat ID
        agentRepo.save(agentStatus); // Save the updated agent status
    }
}