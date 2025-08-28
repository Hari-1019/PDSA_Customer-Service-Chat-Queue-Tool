package com.chatqueue.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
