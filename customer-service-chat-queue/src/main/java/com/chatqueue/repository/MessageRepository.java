package com.chatqueue.repository;

import com.chatqueue.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MessageRepository extends JpaRepository<Message, Integer>{
    List<Message> findByChatIdOrderByTimestampAsc(Integer chatId);
}
