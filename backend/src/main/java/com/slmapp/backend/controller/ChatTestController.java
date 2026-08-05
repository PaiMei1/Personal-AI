package com.slmapp.backend.controller;

import com.slmapp.backend.entity.Message;
import com.slmapp.backend.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/chat")
public class ChatTestController {

    private final ChatOrchestrationService chatOrchestrationService;

    public ChatTestController(ChatOrchestrationService chatOrchestrationService) {
        this.chatOrchestrationService = chatOrchestrationService;
    }

    // userId is hardcoded to 1L for manual testing - swap for the authenticated
    // user's id once this is wired through SecurityContext instead of a raw test endpoint.
    @PostMapping
    public Message ask(@RequestParam String prompt) {
        return chatOrchestrationService.handle(1L, null, prompt);
    }
}
