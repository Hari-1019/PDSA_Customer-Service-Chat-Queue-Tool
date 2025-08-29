package com.chatqueue.service;

import com.chatqueue.enums.*;
import com.chatqueue.model.*;
import com.chatqueue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chats;
    private final MessageRepository msgs;
    private final AgentStatusRepository agents;
    private final QueueManagerService queue;



    public Optional<Chat> assignNextToAgent(UUID agentId){
        // find a pending queue head (VIP first)
        var next = queue.peekNext();
        if(next.isEmpty()) return Optional.empty();

        // create/locate chat for customer and set agent
        var customerId = next.get().getCustomerId();
        // find the last waiting chat for this customer
        var waiting = chats.findByCustomerIdOrderByStartedAtDesc(customerId)
                .stream().filter(c -> c.getStatus()==ChatStatus.waiting).findFirst().orElseThrow();

        waiting.setAgentId(agentId);
        waiting.setStatus(ChatStatus.in_chat);
        waiting.setStartedAt(Instant.now());
        chats.save(waiting);

        // update agent status
        var st = agents.findById(agentId).orElseGet(() -> {
            var a = new AgentStatusRow();
            a.setAgentId(agentId);
            return a;
        });
        st.setCurrentStatus(UserStatus.busy);
        st.setCurrentChatId(waiting.getChatId());
        st.setLastActivity(Instant.now());
        agents.save(st);

        // remove customer from queue and re-number
        queue.removeFromQueue(customerId);

        return Optional.of(waiting);
    }

    public Message send(Integer chatId, UUID senderId, String text){
        var m = new Message();
        m.setChatId(chatId);
        m.setSenderId(senderId);
        m.setMessageText(text);
        return msgs.save(m);
    }

    public List<Message> history(Integer chatId){
        return msgs.findByChatIdOrderByTimestampAsc(chatId);
    }

    public void close(Integer chatId, UUID agentId){
        var chat = chats.findById(chatId).orElseThrow();
        chat.setStatus(ChatStatus.closed);
        chat.setEndedAt(Instant.now());
        chats.save(chat);

        // free agent
        var st = agents.findById(agentId).orElseThrow();
        st.setCurrentStatus(UserStatus.available);
        st.setCurrentChatId(null);
        st.setTotalChatsToday((st.getTotalChatsToday()==null?0:st.getTotalChatsToday())+1);
        st.setLastActivity(Instant.now());
        agents.save(st);
    }
}
