package com.chatqueue.repository;

import com.chatqueue.model.Chat;
import com.chatqueue.enums.ChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChatRepository extends JpaRepository<Chat, Integer>{
    List<Chat> findByCustomerIdOrderByStartedAtDesc(UUID customerId);
    List<Chat> findByAgentIdOrderByStartedAtDesc(UUID agentId);
    long countByStatus(ChatStatus status);

    // Make sure this method exists
    Optional<Chat> findByCustomerIdAndStatus(UUID customerId, ChatStatus status);

    // Add this if missing
    Optional<Chat> findById(Integer chatId);
}