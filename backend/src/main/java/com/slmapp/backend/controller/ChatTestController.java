package com.slmapp.backend.controller;

import com.slmapp.backend.entity.Message;
import com.slmapp.backend.entity.User;
import com.slmapp.backend.repository.UserRepository;
import com.slmapp.backend.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/chat")
public class ChatTestController {

    private static final String TEST_USER_EMAIL = "dev@dev.com";

    private final ChatOrchestrationService chatOrchestrationService;
    private final UserRepository userRepository;

    public ChatTestController(ChatOrchestrationService chatOrchestrationService, UserRepository userRepository) {
        this.chatOrchestrationService = chatOrchestrationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Message ask(@RequestParam String prompt) {
        User testUser = userRepository.findByEmail(TEST_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException(
                        "Test user '" + TEST_USER_EMAIL + "' not found - sign up that email first on this database"));
        return chatOrchestrationService.handle(testUser.getId(), null, prompt);
    }
}