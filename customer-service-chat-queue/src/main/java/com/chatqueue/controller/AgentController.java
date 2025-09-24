package com.chatqueue.controller;

import com.chatqueue.dto.AgentStatusRequest;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.model.User;
import com.chatqueue.repository.AgentStatusRepository;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.service.ChatService;
import com.chatqueue.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class  AgentController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final AgentStatusRepository agentStatusRepository;
    private final JWTUtil jwtUtil;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/login")
    public ResponseEntity<?> loginAgent(@RequestHeader("Authorization") String auth) {
        try {
            String email = jwtUtil.email(auth.substring(7));
            Optional<User> agentOptional = userRepository.findByEmail(email);

            if (agentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Agent not found");
            }

            User agent = agentOptional.get();

            // Set agent status to busy by default on login
            agent.setStatus(UserStatus.busy);
            userRepository.save(agent);

            // Update agent_status table
            AgentStatusRow agentStatus = agentStatusRepository.findById(agent.getUserId())
                    .orElseGet(() -> {
                        AgentStatusRow newStatusRow = new AgentStatusRow();
                        newStatusRow.setAgentId(agent.getUserId());
                        return newStatusRow;
                    });

            agentStatus.setCurrentStatus(UserStatus.busy);
            agentStatus.setLastActivity(Instant.now());
            agentStatusRepository.save(agentStatus);

            return ResponseEntity.ok(Map.of(
                    "message", "Agent logged in successfully",
                    "status", "busy"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error during agent login: " + e.getMessage());
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateAgentStatus(@RequestHeader("Authorization") String auth,
                                               @RequestBody AgentStatusRequest request) {
        try {
            String email = jwtUtil.email(auth.substring(7));
            Optional<User> agentOptional = userRepository.findByEmail(email);

            if (agentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Agent not found");
            }

            User agent = agentOptional.get();

            // Convert string to enum (handle case sensitivity)
            UserStatus newStatus;
            try {
                newStatus = UserStatus.valueOf(request.getStatus().toLowerCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status: " + request.getStatus());
            }

            // Update user table
            agent.setStatus(newStatus);
            userRepository.save(agent);

            // Update agent_status table
            AgentStatusRow agentStatus = agentStatusRepository.findById(agent.getUserId())
                    .orElseGet(() -> {
                        AgentStatusRow newStatusRow = new AgentStatusRow();
                        newStatusRow.setAgentId(agent.getUserId());
                        return newStatusRow;
                    });

            agentStatus.setCurrentStatus(newStatus);
            agentStatus.setLastActivity(Instant.now());

            // If setting to available, clear current chat
            if (newStatus == UserStatus.available) {
                agentStatus.setCurrentChatId(null);

                // Auto-assign next customer in queue when agent becomes available
                Optional<Chat> chatOptional = chatService.assignNextToAgent(agent.getUserId());
                if (chatOptional.isPresent()) {
                    Chat chat = chatOptional.get();

                    // Notify customer that an agent has been assigned
                    messagingTemplate.convertAndSend("/queue/customer/" + chat.getCustomerId(),
                        Map.of(
                            "type", "agent_assigned",
                            "chatId", chat.getChatId(),
                            "agentName", agent.getName(),
                            "message", "Agent " + agent.getName() + " has been assigned to your chat."
                        ));

                    // Get customer name for the UI
                    String customerName = "Customer";
                    Optional<User> customerOptional = userRepository.findById(chat.getCustomerId());
                    if (customerOptional.isPresent()) {
                        customerName = customerOptional.get().getName();
                    }

                    // Return the chat details to be displayed in the agent's UI
                    return ResponseEntity.ok(Map.of(
                        "status", newStatus.name(),
                        "message", "Status updated successfully and customer assigned",
                        "chatAssigned", true,
                        "chatId", chat.getChatId(),
                        "customerId", chat.getCustomerId(),
                        "customerName", customerName,
                        "customerQuery", chat.getCustomerQuery()
                    ));
                }
            }

            agentStatusRepository.save(agentStatus);

            return ResponseEntity.ok(Map.of(
                    "status", newStatus.name(),
                    "message", "Status updated successfully",
                    "chatAssigned", false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }

    @GetMapping("/next-customer")
    public ResponseEntity<?> getNextCustomer(@RequestHeader("Authorization") String auth) {
        try {
            String email = jwtUtil.email(auth.substring(7));
            Optional<User> agentOptional = userRepository.findByEmail(email);

            if (agentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Agent not found");
            }

            User agent = agentOptional.get();
            if (!"available".equals(agent.getStatus().name())) {
                return ResponseEntity.badRequest().body("Agent must be available to get next customer");
            }

            Optional<Chat> chatOptional = chatService.assignNextToAgent(agent.getUserId());
            if (chatOptional.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No customers in queue"));
            }

            Chat chat = chatOptional.get();

            // Get customer name for the UI
            String customerName = "Customer";
            Optional<User> customerOptional = userRepository.findById(chat.getCustomerId());
            if (customerOptional.isPresent()) {
                customerName = customerOptional.get().getName();
            }

            // Notify customer that an agent has been assigned
            messagingTemplate.convertAndSend("/queue/customer/" + chat.getCustomerId(),
                Map.of(
                    "type", "agent_assigned",
                    "chatId", chat.getChatId(),
                    "agentName", agent.getName(),
                    "message", "Agent " + agent.getName() + " has been assigned to your chat."
                ));

            return ResponseEntity.ok(Map.of(
                    "chatId", chat.getChatId(),
                    "customerId", chat.getCustomerId(),
                    "customerName", customerName,
                    "customerQuery", chat.getCustomerQuery()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting next customer: " + e.getMessage());
        }
    }

    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestHeader("Authorization") String auth,
                                         @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.email(auth.substring(7));
            Optional<User> agentOptional = userRepository.findByEmail(email);

            if (agentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Agent not found");
            }

            User agent = agentOptional.get();
            Integer chatId = (Integer) request.get("chatId");
            String messageText = (String) request.get("messageText");

            Message message = chatService.send(chatId, agent.getUserId(), messageText);

            // Send message through WebSocket to the customer
            Chat chat = chatRepository.findById(chatId).orElse(null);
            if (chat != null) {
                messagingTemplate.convertAndSend("/queue/customer/" + chat.getCustomerId(),
                    Map.of(
                        "type", "new_message",
                        "chatId", chatId,
                        "messageId", message.getMessageId(),
                        "senderId", agent.getUserId(),
                        "senderName", agent.getName(),
                        "messageText", messageText,
                        "timestamp", message.getTimestamp()
                    ));
            }

            return ResponseEntity.ok(Map.of("messageId", message.getMessageId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending message: " + e.getMessage());
        }
    }

    @PostMapping("/end-chat")
    public ResponseEntity<?> endChat(@RequestHeader("Authorization") String auth,
                                     @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.email(auth.substring(7));
            Optional<User> agentOptional = userRepository.findByEmail(email);

            if (agentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Agent not found");
            }

            User agent = agentOptional.get();
            Integer chatId = (Integer) request.get("chatId");

            Chat chat = chatRepository.findById(chatId).orElse(null);
            if (chat == null) {
                return ResponseEntity.badRequest().body("Chat not found");
            }

            UUID customerId = chat.getCustomerId();
            chatService.close(chatId, agent.getUserId());

            // Notify customer that the chat has ended
            messagingTemplate.convertAndSend("/queue/customer/" + customerId,
                Map.of(
                    "type", "chat_ended",
                    "chatId", chatId,
                    "agentName", agent.getName(),
                    "message", "Chat has been ended by Agent " + agent.getName()
                ));

            // Auto-assign next customer after ending current chat if agent is available
            AgentStatusRow agentStatus = agentStatusRepository.findById(agent.getUserId()).orElse(null);
            if (agentStatus != null && UserStatus.available.equals(agentStatus.getCurrentStatus())) {
                Optional<Chat> nextChatOptional = chatService.assignNextToAgent(agent.getUserId());
                if (nextChatOptional.isPresent()) {
                    Chat nextChat = nextChatOptional.get();

                    // Get customer name for the UI
                    String customerName = "Customer";
                    Optional<User> customerOptional = userRepository.findById(nextChat.getCustomerId());
                    if (customerOptional.isPresent()) {
                        customerName = customerOptional.get().getName();
                    }

                    // Notify customer that an agent has been assigned
                    messagingTemplate.convertAndSend("/queue/customer/" + nextChat.getCustomerId(),
                        Map.of(
                            "type", "agent_assigned",
                            "chatId", nextChat.getChatId(),
                            "agentName", agent.getName(),
                            "message", "Agent " + agent.getName() + " has been assigned to your chat."
                        ));

                    return ResponseEntity.ok(Map.of(
                        "message", "Chat ended successfully and new customer assigned",
                        "chatEnded", true,
                        "newChatAssigned", true,
                        "chatId", nextChat.getChatId(),
                        "customerId", nextChat.getCustomerId(),
                        "customerName", customerName,
                        "customerQuery", nextChat.getCustomerQuery()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Chat ended successfully",
                "chatEnded", true,
                "newChatAssigned", false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error ending chat: " + e.getMessage());
        }
    }

    @GetMapping("/chat-history/{chatId}")
    public ResponseEntity<?> getChatHistory(@RequestHeader("Authorization") String auth,
                                            @PathVariable Integer chatId) {
        try {
            List<Message> messages = chatService.history(chatId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting chat history: " + e.getMessage());
        }
    }

    @GetMapping("/new-messages/{chatId}")
    public ResponseEntity<?> getNewMessages(@RequestHeader("Authorization") String auth,
                                            @PathVariable Integer chatId) {
        try {
            // Get messages from the last 5 seconds
            Instant since = Instant.now().minusSeconds(5);
            List<Message> newMessages = messageRepository.findByChatIdAndTimestampAfter(chatId, since);
            return ResponseEntity.ok(newMessages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting new messages: " + e.getMessage());
        }
    }
}