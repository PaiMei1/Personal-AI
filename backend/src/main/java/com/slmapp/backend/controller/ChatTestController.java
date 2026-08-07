package com.slmapp.backend.controller;

import com.slmapp.backend.entity.Message;
import com.slmapp.backend.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/test/chat")
public class ChatTestController {

    // Hardcoded to a real user id from the dev DB for manual testing.
    // Swap for the authenticated user's id once this is wired through SecurityContext.
    private static final UUID TEST_USER_ID = UUID.fromString("611ea423-a192-4472-b8fb-e268915f7136");

    private final ChatOrchestrationService chatOrchestrationService;

    public ChatTestController(ChatOrchestrationService chatOrchestrationService) {
        this.chatOrchestrationService = chatOrchestrationService;
    }

    @PostMapping
    public Message ask(@RequestParam String prompt) {
        return chatOrchestrationService.handle(TEST_USER_ID, null, prompt);
    }
}
