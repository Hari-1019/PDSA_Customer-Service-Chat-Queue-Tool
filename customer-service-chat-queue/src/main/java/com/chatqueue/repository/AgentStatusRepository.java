package com.chatqueue.repository;

import com.chatqueue.model.AgentStatusRow;
import com.chatqueue.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AgentStatusRepository extends JpaRepository<AgentStatusRow, UUID> {
    List<AgentStatusRow> findByCurrentStatus(UserStatus status);
    List<AgentStatusRow> findAll();

    // Add this method if it doesn't exist
    Optional<AgentStatusRow> findById(UUID agentId);
}