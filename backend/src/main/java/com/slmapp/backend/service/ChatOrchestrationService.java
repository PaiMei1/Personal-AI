package com.slmapp.backend.service;

import com.slmapp.backend.entity.Conversation;
import com.slmapp.backend.entity.Message;
import com.slmapp.backend.entity.MessageStatus;
import com.slmapp.backend.magi.ComplexityClassifier;
import com.slmapp.backend.repository.ConversationRepository;
import com.slmapp.backend.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private final ComplexityClassifier complexityClassifier;
    private final MagiAsyncRunner magiAsyncRunner;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatOrchestrationService(ComplexityClassifier complexityClassifier,
                                    MagiAsyncRunner magiAsyncRunner,
                                    ConversationRepository conversationRepository,
                                    MessageRepository messageRepository) {
        this.complexityClassifier = complexityClassifier;
        this.magiAsyncRunner = magiAsyncRunner;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public boolean requiresModeSelection(String prompt) {
        return complexityClassifier.isComplex(prompt);
    }

    public Message start(UUID userId, UUID conversationId, String prompt, String mode) {
        Conversation conversation = conversationId != null
                ? conversationRepository.findById(conversationId).orElseThrow()
                : conversationRepository.save(new Conversation(userId, null));

        messageRepository.save(new Message(conversation.getId(), "user", prompt));

        Message assistantMessage = new Message(conversation.getId(), "assistant", "");
        assistantMessage.setStatus(MessageStatus.PENDING);
        messageRepository.save(assistantMessage);

        // Calling through the injected MagiAsyncRunner bean, not on `this` -
        // required for @Async to actually take effect.
        magiAsyncRunner.runAsync(assistantMessage.getId(), prompt, mode);

        return assistantMessage;
    }
}