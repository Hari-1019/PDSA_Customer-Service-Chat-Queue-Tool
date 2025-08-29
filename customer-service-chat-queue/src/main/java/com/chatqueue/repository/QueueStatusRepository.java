package com.chatqueue.repository;

import com.chatqueue.model.QueueStatusRow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface QueueStatusRepository extends JpaRepository<QueueStatusRow, Integer>{
    List<QueueStatusRow> findByQueueTypeOrderByPositionAsc(String queueType);
    Optional<QueueStatusRow> findByCustomerId(UUID customerId);
    long countByQueueType(String queueType);
}
