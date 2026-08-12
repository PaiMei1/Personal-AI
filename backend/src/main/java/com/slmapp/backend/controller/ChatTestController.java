package com.slmapp.backend.controller;

import com.slmapp.backend.dto.ChatPrepareRequest;
import com.slmapp.backend.dto.ChatRequest;
import com.slmapp.backend.entity.Message;
import com.slmapp.backend.entity.User;
import com.slmapp.backend.repository.MagiVerdictRepository;
import com.slmapp.backend.repository.MessageRepository;
import com.slmapp.backend.repository.UserRepository;
import com.slmapp.backend.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test/chat")
public class ChatTestController {

    private static final String TEST_USER_EMAIL = "dev@dev.com";

    private final ChatOrchestrationService chatOrchestrationService;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MagiVerdictRepository magiVerdictRepository;

    public ChatTestController(ChatOrchestrationService chatOrchestrationService,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               MagiVerdictRepository magiVerdictRepository) {
        this.chatOrchestrationService = chatOrchestrationService;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.magiVerdictRepository = magiVerdictRepository;
    }

    @PostMapping("/prepare")
    public Map<String, Boolean> prepare(@RequestBody ChatPrepareRequest request) {
        return Map.of("requiresModeSelection", chatOrchestrationService.requiresModeSelection(request.prompt()));
    }

    @PostMapping
    public Message ask(@RequestBody ChatRequest request) {
        User testUser = userRepository.findByEmail(TEST_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException(
                        "Test user '" + TEST_USER_EMAIL + "' not found - sign up that email first on this database"));
        return chatOrchestrationService.start(testUser.getId(), null, request.prompt(), request.mode());
    }

    @GetMapping("/{messageId}/status")
    public Map<String, Object> status(@PathVariable UUID messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        return Map.of(
                "status", message.getStatus(),
                "content", message.getContent(),
                "decisionMode", message.getDecisionMode(),
                "consensusResult", message.getConsensusResult() != null ? message.getConsensusResult() : "",
                "verdicts", magiVerdictRepository.findByMessageIdOrderByRoundAsc(messageId)
        );
    }
}
