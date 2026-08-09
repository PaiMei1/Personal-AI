package com.slmapp.backend.service;

import com.slmapp.backend.entity.*;
import com.slmapp.backend.magi.ComplexityClassifier;
import com.slmapp.backend.magi.MagiCandidateAnswer;
import com.slmapp.backend.magi.MagiCoordinator;
import com.slmapp.backend.magi.MagiEventPublisher;
import com.slmapp.backend.magi.MagiOutcome;
import com.slmapp.backend.magi.MagiPersona;
import com.slmapp.backend.magi.MagiVote;
import com.slmapp.backend.magi.events.MagiDecisionEvent;
import com.slmapp.backend.magi.events.MagiSynthesisEvent;
import com.slmapp.backend.repository.ConversationRepository;
import com.slmapp.backend.repository.MagiSynthesisRepository;
import com.slmapp.backend.repository.MagiVerdictRepository;
import com.slmapp.backend.repository.MessageRepository;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private final SlmClient slmClient;
    private final ComplexityClassifier complexityClassifier;
    private final MagiCoordinator magiCoordinator;
    private final MagiEventPublisher eventPublisher;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MagiVerdictRepository magiVerdictRepository;
    private final MagiSynthesisRepository magiSynthesisRepository;

    public ChatOrchestrationService(SlmClient slmClient,
                                     ComplexityClassifier complexityClassifier,
                                     MagiCoordinator magiCoordinator,
                                     MagiEventPublisher eventPublisher,
                                     ConversationRepository conversationRepository,
                                     MessageRepository messageRepository,
                                     MagiVerdictRepository magiVerdictRepository,
                                     MagiSynthesisRepository magiSynthesisRepository) {
        this.slmClient = slmClient;
        this.complexityClassifier = complexityClassifier;
        this.magiCoordinator = magiCoordinator;
        this.eventPublisher = eventPublisher;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.magiVerdictRepository = magiVerdictRepository;
        this.magiSynthesisRepository = magiSynthesisRepository;
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

        runAsync(assistantMessage.getId(), prompt, mode);

        return assistantMessage;
    }

    @Async
    public void runAsync(UUID messageId, String prompt, String mode) {
        Message assistantMessage = messageRepository.findById(messageId).orElseThrow();
        assistantMessage.setStatus(MessageStatus.IN_PROGRESS);
        messageRepository.save(assistantMessage);

        try {
            if ("MAGI".equals(mode)) {
                runMagi(assistantMessage, prompt);
            } else if (mode != null && mode.startsWith("SINGLE:")) {
                runSingleCore(assistantMessage, prompt, mode.substring("SINGLE:".length()));
            } else {
                runDirect(assistantMessage, prompt);
            }
            assistantMessage.setStatus(MessageStatus.COMPLETE);
            messageRepository.save(assistantMessage);
        } catch (Exception e) {
            assistantMessage.setStatus(MessageStatus.FAILED);
            assistantMessage.setContent("Something went wrong while generating this response.");
            messageRepository.save(assistantMessage);
        }
    }

    private void runDirect(Message assistantMessage, String prompt) {
        String answer = slmClient.complete("You are a helpful assistant.", prompt, null);
        assistantMessage.setContent(answer);
        assistantMessage.setDecisionMode(DecisionMode.DIRECT);
    }

    private void runSingleCore(Message assistantMessage, String prompt, String unitName) {
        MagiPersona persona = MagiPersona.valueOf(unitName);
        String answer = slmClient.complete(persona.systemPrompt(), prompt, null);
        assistantMessage.setContent(answer);
        assistantMessage.setDecisionMode(DecisionMode.DIRECT);
        assistantMessage.setWinningUnit(persona.unit());
    }

    private void runMagi(Message assistantMessage, String prompt) {
        MagiOutcome outcome = magiCoordinator.run(assistantMessage.getId(), prompt);

        String finalAnswer = outcome.consensusResult() == ConsensusResult.SPLIT
                ? "MAGI did not reach consensus. See the differences breakdown."
                : outcome.finalAnswer();

        assistantMessage.setContent(finalAnswer);
        assistantMessage.setDecisionMode(DecisionMode.MAGI);
        assistantMessage.setConsensusResult(outcome.consensusResult());
        assistantMessage.setWinningUnit(outcome.winningUnit());

        for (MagiCandidateAnswer candidate : outcome.candidates()) {
            magiVerdictRepository.save(new MagiVerdict(
                    assistantMessage.getId(), candidate.unit(), 1, candidate.answer(), null, null));
        }
        for (MagiVote vote : outcome.votes()) {
            magiVerdictRepository.save(new MagiVerdict(
                    assistantMessage.getId(), vote.voter(), 2, vote.justification(), vote.votedFor(), vote.confidence()));
        }

        if (outcome.consensusResult() == ConsensusResult.SPLIT) {
            magiSynthesisRepository.save(new MagiSynthesis(
                    assistantMessage.getId(), outcome.commonGroundJson(), outcome.differencesJson()));
        }

        eventPublisher.decision(new MagiDecisionEvent(
                assistantMessage.getId(), outcome.consensusResult(), outcome.winningUnit()));

        if (outcome.consensusResult() == ConsensusResult.SPLIT) {
            eventPublisher.synthesis(new MagiSynthesisEvent(
                    assistantMessage.getId(), outcome.commonGroundJson(), outcome.differencesJson()));
        }
    }
}
