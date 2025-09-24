package com.chatqueue.service;

import com.chatqueue.model.User;
import com.chatqueue.model.QueueStatusRow;
import com.chatqueue.repository.QueueStatusRepository;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.enums.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QueueManagerService {

    private final QueueStatusRepository queueStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public int addToQueue(User customer, Integer chatId) {
        // Remove any existing queue entry for this customer
        queueStatusRepository.findByCustomerId(customer.getUserId())
                .ifPresent(queueStatusRepository::delete);

        // Determine queue type based on customer account type - use lowercase enum values
        String queueType = customer.getAccountType() == AccountType.enterprise ?
                "enterprise_vip" : "individual_normal";

        // Get current queue length for this type
        int queueLength = (int) queueStatusRepository.countByQueueType(queueType);
        int position = queueLength + 1;

        // Calculate estimated wait time
        int estimatedWait = calculateEstimatedWaitTime(queueType, position);

        // Create queue status entry
        QueueStatusRow queueStatus = new QueueStatusRow();
        queueStatus.setQueueType(queueType);
        queueStatus.setCustomerId(customer.getUserId());
        queueStatus.setPosition(position);
        queueStatus.setEnqueuedAt(Instant.now());
        queueStatus.setEstimatedWaitMinutes(estimatedWait);

        queueStatusRepository.save(queueStatus);

        return position;
    }

    public int getQueuePosition(UUID customerId) {
        return queueStatusRepository.findByCustomerId(customerId)
                .map(QueueStatusRow::getPosition)
                .orElse(0);
    }

    public int getEstimatedWaitTime(UUID customerId) {
        return queueStatusRepository.findByCustomerId(customerId)
                .map(QueueStatusRow::getEstimatedWaitMinutes)
                .orElse(0);
    }

    @Transactional
    public void removeFromQueue(UUID customerId) {
        // Remove customer from queue
        Optional<QueueStatusRow> queueStatusOptional = queueStatusRepository.findByCustomerId(customerId);
        if (queueStatusOptional.isPresent()) {
            QueueStatusRow queueStatus = queueStatusOptional.get();
            String queueType = queueStatus.getQueueType();

            // Remove this customer
            queueStatusRepository.delete(queueStatus);

            // Recalculate positions for remaining customers in the same queue
            recalculateQueuePositions(queueType);
        }
    }

    private void recalculateQueuePositions(String queueType) {
        List<QueueStatusRow> queueEntries = queueStatusRepository.findByQueueTypeOrderByPositionAsc(queueType);

        for (int i = 0; i < queueEntries.size(); i++) {
            QueueStatusRow entry = queueEntries.get(i);
            entry.setPosition(i + 1);
            entry.setEstimatedWaitMinutes(calculateEstimatedWaitTime(queueType, i + 1));
            queueStatusRepository.save(entry);
        }
    }

    // Get the next customer from the queue (VIP first, then normal)
    public Optional<QueueStatusRow> peekNext() {
        // First check VIP queue
        List<QueueStatusRow> vipQueue = queueStatusRepository.findByQueueTypeOrderByPositionAsc("enterprise_vip");
        if (!vipQueue.isEmpty()) {
            return Optional.of(vipQueue.get(0));
        }

        // If no VIP, check normal queue
        List<QueueStatusRow> normalQueue = queueStatusRepository.findByQueueTypeOrderByPositionAsc("individual_normal");
        if (!normalQueue.isEmpty()) {
            return Optional.of(normalQueue.get(0));
        }

        return Optional.empty();
    }

    // Get queue lengths for admin dashboard
    public Map<String, Long> lengths() {
        Map<String, Long> lengths = new HashMap<>();
        lengths.put("enterprise_vip", queueStatusRepository.countByQueueType("enterprise_vip"));
        lengths.put("individual_normal", queueStatusRepository.countByQueueType("individual_normal"));
        return lengths;
    }



    private int calculateEstimatedWaitTime(String queueType, int position) {
        // Simple estimation: 2 minutes per position for VIP, 5 minutes for normal
        return queueType.equals("enterprise_vip") ? position * 2 : position * 5;
    }
    // In QueueManagerService.java, add this method:
    public long getQueueCount(String queueType) {
        return queueStatusRepository.countByQueueType(queueType);
    }


}