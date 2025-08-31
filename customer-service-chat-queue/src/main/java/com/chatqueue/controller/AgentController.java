/*package com.chatqueue.controller;


import com.chatqueue.dto.ChatMessageRequest;
import com.chatqueue.model.*;
import com.chatqueue.repository.UserRepository;
import com.chatqueue.service.ChatService;
import com.chatqueue.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    private final ChatService chats;
    private final UserRepository users;
    private final JWTUtil jwt;

    // 1) Assign next CX (VIP first)
    @PostMapping("/assign-next")
    public ResponseEntity<?> assign(@RequestHeader("Authorization") String auth){
        var email = jwt.email(auth.substring(7));
        var agent = users.findByEmail(email).orElseThrow();
        var assigned = chats.assignNextToAgent(agent.getUserId());
        return ResponseEntity.ok(Map.of(
                "assigned", assigned.isPresent(),
                "chat", assigned.orElse(null)
        ));
    }

    // 2) Send message (agent)
    @PostMapping("/chat/{chatId}/message")
    public ResponseEntity<?> send(@RequestHeader("Authorization") String auth,
                                  @PathVariable Integer chatId,
                                  @RequestBody ChatMessageRequest req){
        var email = jwt.email(auth.substring(7));
        var agent = users.findByEmail(email).orElseThrow();
        var m = chats.send(chatId, agent.getUserId(), req.getMessage());
        return ResponseEntity.ok(m);
    }

    // 3) Close chat
    @PostMapping("/chat/{chatId}/close")
    public ResponseEntity<?> close(@RequestHeader("Authorization") String auth,
                                   @PathVariable Integer chatId){
        var email = jwt.email(auth.substring(7));
        var agent = users.findByEmail(email).orElseThrow();
        chats.close(chatId, agent.getUserId());
        return ResponseEntity.ok(Map.of("closed", true));
    }

    // 4) History (for UI)
    @GetMapping("/chat/{chatId}/messages")
    public ResponseEntity<?> history(@PathVariable Integer chatId){
        return ResponseEntity.ok(chats.history(chatId));
    }
}
*/