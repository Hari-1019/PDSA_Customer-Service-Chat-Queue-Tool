package com.chatqueue.controller;

import com.chatqueue.dto.ChatEndRequest;
import com.chatqueue.model.Message;
import com.chatqueue.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public Message sendMessage(Message message) {
        // Save message to database
        Message savedMessage = chatService.send(message.getChatId(), message.getSenderId(), message.getMessageText());

        // Notify specific chat participants
        messagingTemplate.convertAndSend("/queue/chat/" + message.getChatId(), savedMessage);

        return savedMessage;
    }

    @MessageMapping("/chat.end")
    public void endChat(ChatEndRequest request) {
        chatService.close(request.getChatId(), request.getAgentId());

        // Notify both agent and customer that chat ended
        messagingTemplate.convertAndSend("/queue/chat-ended/" + request.getChatId(),
                "Chat ended by " + (request.getAgentId() != null ? "agent" : "customer"));
    }
}