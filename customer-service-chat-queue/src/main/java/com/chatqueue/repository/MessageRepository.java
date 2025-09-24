package com.chatqueue.repository;

import com.chatqueue.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer>{
    List<Message> findByChatIdOrderByTimestampAsc(Integer chatId);

    @Query("SELECT m FROM Message m WHERE m.chatId = :chatId AND m.timestamp > :since ORDER BY m.timestamp ASC")
    List<Message> findByChatIdAndTimestampAfter(@Param("chatId") Integer chatId, @Param("since") Instant since);
}