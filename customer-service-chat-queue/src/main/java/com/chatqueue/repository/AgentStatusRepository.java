package com.chatqueue.repository;

import com.chatqueue.model.AgentStatus;
import com.chatqueue.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentStatusRepository extends JpaRepository<AgentStatus, Long> {
    List<AgentStatus> findByStatus(UserStatus status);
}
