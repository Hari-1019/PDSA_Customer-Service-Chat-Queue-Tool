package com.chatqueue.repository;

import com.chatqueue.model.QueueStatusRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueStatusRepository extends JpaRepository<QueueStatusRow, Integer> {
    List<QueueStatusRow> findByQueueTypeOrderByPositionAsc(String queueType); // This should return List, not Optional
    Optional<QueueStatusRow> findByCustomerId(UUID customerId);
    long countByQueueType(String queueType);

    @Query("SELECT MIN(q.position) FROM QueueStatusRow q WHERE q.queueType = :queueType")
    Optional<Integer> findMinPositionByQueueType(@Param("queueType") String queueType);
    
}