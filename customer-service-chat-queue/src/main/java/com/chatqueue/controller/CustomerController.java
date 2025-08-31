package com.chatqueue.controller;

import com.chatqueue.dto.ChatMessageRequest;
import com.chatqueue.enums.ChatStatus;
import com.chatqueue.enums.UserStatus;
import com.chatqueue.model.Chat;
import com.chatqueue.model.Message;
import com.chatqueue.model.QueueStatusRow;
import com.chatqueue.model.User;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.MessageRepository;
import com.chatqueue.repository.QueueStatusRepository;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.service.QueueManagerService;
import com.chatqueue.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserRepository users;
    private final MessageRepository messages;
    private final ChatRepository chatRepository;
    private final QueueStatusRepository queueRepo;
    private final QueueManagerService queueManagerService;
    private final JWTUtil jwt;

    /**
     * Send message and create chat if it doesn't exist
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestHeader("Authorization") String auth,
                                         @RequestBody ChatMessageRequest body) {
        try {
            // Extract user from JWT
            var email = jwt.email(auth.substring(7));
            var user = users.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

            // Find or create chat
            Chat chat;
            Optional<Chat> existingChat = chatRepository.findByCustomerIdAndStatus(user.getUserId(), ChatStatus.waiting);

            if (existingChat.isPresent()) {
                chat = existingChat.get();
            } else {
                // Create new chat
                chat = new Chat();
                chat.setCustomerId(user.getUserId());
                chat.setCustomerQuery(body.getMessageText());
                chat.setStatus(ChatStatus.waiting);
                chat.setStartedAt(Instant.now());
                chat = chatRepository.save(chat);

                // Add to queue (only once per chat)
                queueManagerService.addToQueue(user, chat.getChatId());
            }

            // Save message to DB
            var message = new Message();
            message.setChatId(chat.getChatId());
            message.setSenderId(user.getUserId());
            message.setMessageText(body.getMessageText());
            message.setMessageType("text");
            message.setTimestamp(Instant.now());
            messages.save(message);

            return ResponseEntity.ok(Map.of(
                    "message", "Message sent successfully",
                    "chatId", chat.getChatId(),
                    "queuePosition", queueManagerService.getQueuePosition(user.getUserId())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get queue position for customer
     */
    @GetMapping("/queue-position")
    public ResponseEntity<?> getQueuePosition(@RequestHeader("Authorization") String auth) {
        try {
            var email = jwt.email(auth.substring(7));
            var user = users.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

            int position = queueManagerService.getQueuePosition(user.getUserId());
            int estimatedWait = queueManagerService.getEstimatedWaitTime(user.getUserId());

            return ResponseEntity.ok(Map.of(
                    "position", position,
                    "estimatedWaitMinutes", estimatedWait
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get queue summary for admin dashboard
     */
    @GetMapping("/queue-summary")
    public ResponseEntity<?> getQueueSummary() {
        long vipCount = queueRepo.countByQueueType("enterprise_vip");
        long normalCount = queueRepo.countByQueueType("individual_normal");

        return ResponseEntity.ok(Map.of(
                "vipCustomers", vipCount,
                "normalCustomers", normalCount
        ));
    }
    @PostMapping("/close-chat")
    public ResponseEntity<?> closeChat(@RequestHeader("Authorization") String auth) {
        try {
            // Extract user from JWT
            var email = jwt.email(auth.substring(7));
            var user = users.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

            // Remove from queue
            queueManagerService.removeFromQueue(user.getUserId());

            // Find and update chat status
            Optional<Chat> chatOptional = chatRepository.findByCustomerIdAndStatus(user.getUserId(), ChatStatus.waiting);
            if (chatOptional.isPresent()) {
                Chat chat = chatOptional.get();
                chat.setStatus(ChatStatus.closed);
                chat.setEndedAt(Instant.now());
                chatRepository.save(chat);
            }

            // Update user status
            user.setStatus(UserStatus.available);
            users.save(user);

            return ResponseEntity.ok("Chat closed and removed from queue");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error closing chat: " + e.getMessage());
        }
    }
}