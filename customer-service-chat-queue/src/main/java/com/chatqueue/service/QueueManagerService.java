package com.chatqueue.service;

import com.chatqueue.dto.QueuePositionResponse;
import com.chatqueue.enums.ChatStatus;
import com.chatqueue.model.Chat;
import com.chatqueue.model.QueueStatusRow;
import com.chatqueue.repository.ChatRepository;
import com.chatqueue.repository.QueueStatusRepository;
import com.chatqueue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QueueManagerService {
    private final QueueStatusRepository queueRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;

    /**
     * Enqueues a customer into the appropriate queue (VIP or Normal) based on their priority level.
     * Creates a new chat entry and queue status entry.
     */
    public QueuePositionResponse enqueue(UUID customerId, String initialQuery) {
        var user = userRepo.findById(customerId).orElseThrow();
        var queueType = user.getPriorityLevel() == 2 ? "enterprise_vip" : "individual_normal";

        // Calculate position in the queue
        int position = (int) queueRepo.countByQueueType(queueType) + 1;

        // Create a new chat entry
        var chat = new Chat();
        chat.setCustomerId(customerId);
        chat.setQueuePosition(position);
        chat.setCustomerQuery(initialQuery);
        chat.setStatus(ChatStatus.waiting);
        chatRepo.save(chat);

        // Create a new queue status entry
        var queueStatus = new QueueStatusRow();
        queueStatus.setQueueType(queueType);
        queueStatus.setCustomerId(customerId);
        queueStatus.setPosition(position);
        queueStatus.setEstimatedWaitMinutes(position * 2); // Example: 2 minutes per position
        queueRepo.save(queueStatus);

        return new QueuePositionResponse(chat.getChatId(), position);
    }

    /**
     * Retrieves the next customer in the queue (VIP first, then Normal).
     */
    public Optional<QueueStatusRow> peekNext() {
        var vipQueue = queueRepo.findByQueueTypeOrderByPositionAsc("enterprise_vip");
        if (!vipQueue.isEmpty()) return Optional.of(vipQueue.get(0));
        var normalQueue = queueRepo.findByQueueTypeOrderByPositionAsc("individual_normal");
        return normalQueue.isEmpty() ? Optional.empty() : Optional.of(normalQueue.get(0));
    }

    /**
     * Removes a customer from the queue and reorders the remaining queue.
     */
    public void removeFromQueue(UUID customerId) {
        queueRepo.findByCustomerId(customerId).ifPresent(queue -> {
            queueRepo.delete(queue);
            var remainingQueue = queueRepo.findByQueueTypeOrderByPositionAsc(queue.getQueueType());
            for (int i = 0; i < remainingQueue.size(); i++) {
                remainingQueue.get(i).setPosition(i + 1);
            }
            queueRepo.saveAll(remainingQueue);
        });
    }

    /**
     * Returns the lengths of both VIP and Normal queues.
     */
    public Map<String, Long> lengths() {
        return Map.of(
                "vip", queueRepo.countByQueueType("enterprise_vip"),
                "normal", queueRepo.countByQueueType("individual_normal")
        );
    }
}