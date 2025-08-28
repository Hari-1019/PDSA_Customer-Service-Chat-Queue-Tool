package com.chatqueue.controller;

import com.chatqueue.dto.ChatMessageRequest;
import com.chatqueue.dto.SubmitQueryRequest;
import com.chatqueue.service.QueueManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final QueueManagerService svc;

    @MessageMapping("/cx.enter")
    public void cxEnter(SubmitQueryRequest req) {
        svc.enqueue(req.tempId(), req.displayName(), req.message(), req.customerType());
    }

    public record AgentAvail(Long agentId, boolean available) {}

    @MessageMapping("/agent.available")
    public void agentAvailable(AgentAvail req) {
        svc.setAgentAvailability(req.agentId(), req.available());
    }

    @MessageMapping("/chat.send")
    public void chatSend(ChatMessageRequest req) {
        svc.receiveChat(req.sessionId(), req.fromRole(), req.content());
    }

    public record CloseReq(Long sessionId) {}

    @MessageMapping("/chat.close")
    public void chatClose(CloseReq req) {
        svc.closeChat(req.sessionId());
    }
}
