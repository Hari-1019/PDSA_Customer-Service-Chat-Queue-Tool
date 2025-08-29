package com.chatqueue.controller;


import com.chatqueue.dto.ChatMessageRequest;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    @MessageMapping("/sendMessage")
    @SendTo("/topic/public")
    public ChatMessageRequest sendMessage(ChatMessageRequest message) {
        return message;
    }
}
