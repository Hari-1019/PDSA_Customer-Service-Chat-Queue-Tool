package com.chatqueue.service;

import com.chatqueue.dto.QueuePositionResponse;
import com.chatqueue.enums.*;
import com.chatqueue.model.*;
import com.chatqueue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;


@Service @RequiredArgsConstructor
public class QueueManagerService {
    private final QueueStatusRepository queueRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;

    public QueuePositionResponse enqueue(UUID customerId, String initialQuery){
        var user = userRepo.findById(customerId).orElseThrow();
        var queueType = user.getPriorityLevel()!=null && user.getPriorityLevel()==2
                ? "enterprise_vip" : "individual_normal";

        // Position = 1 + existing rows in that queue
        int pos = (int) queueRepo.countByQueueType(queueType) + 1;

        // Create Chat row (waiting)
        var chat = new Chat();
        chat.setCustomerId(customerId);
        chat.setQueuePosition(pos);
        chat.setCustomerQuery(initialQuery);
        chat.setStatus(ChatStatus.waiting);
        chatRepo.save(chat);

        // Insert queue row
        var q = new QueueStatusRow();
        q.setQueueType(queueType);
        q.setCustomerId(customerId);
        q.setPosition(pos);
        q.setEstimatedWaitMinutes(Math.max(0, (pos-1)*2)); // naive estimate 2 min each
        queueRepo.save(q);

        return new QueuePositionResponse(chat.getChatId(), pos);
    }

    public Optional<QueueStatusRow> peekNext(){
        var vip = queueRepo.findByQueueTypeOrderByPositionAsc("enterprise_vip");
        if(!vip.isEmpty()) return Optional.of(vip.get(0));
        var normal = queueRepo.findByQueueTypeOrderByPositionAsc("individual_normal");
        return normal.isEmpty()? Optional.empty() : Optional.of(normal.get(0));
    }

    public void removeFromQueue(UUID customerId){
        queueRepo.findByCustomerId(customerId).ifPresent(row -> {
            queueRepo.delete(row);
            // re-number the remaining queue of that type
            var all = queueRepo.findByQueueTypeOrderByPositionAsc(row.getQueueType());
            for (int i=0;i<all.size();i++) {
                all.get(i).setPosition(i+1);
            }
            queueRepo.saveAll(all);
        });
    }

    public Map<String,Long> lengths(){
        return Map.of(
                "vip", queueRepo.countByQueueType("enterprise_vip"),
                "normal", queueRepo.countByQueueType("individual_normal")
        );
    }

}
