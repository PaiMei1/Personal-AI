package com.slmapp.backend.service;

import com.slmapp.backend.entity.*;
import com.slmapp.backend.magi.ComplexityClassifier;
import com.slmapp.backend.magi.MagiCandidateAnswer;
import com.slmapp.backend.magi.MagiCoordinator;
import com.slmapp.backend.magi.MagiOutcome;
import com.slmapp.backend.magi.MagiVote;
import com.slmapp.backend.repository.ConversationRepository;
import com.slmapp.backend.repository.MagiSynthesisRepository;
import com.slmapp.backend.repository.MagiVerdictRepository;
import com.slmapp.backend.repository.MessageRepository;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private final SlmClient slmClient;
    private final ComplexityClassifier complexityClassifier;
    private final MagiCoordinator magiCoordinator;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MagiVerdictRepository magiVerdictRepository;
    private final MagiSynthesisRepository magiSynthesisRepository;

    public ChatOrchestrationService(SlmClient slmClient,
                                     ComplexityClassifier complexityClassifier,
                                     MagiCoordinator magiCoordinator,
                                     ConversationRepository conversationRepository,
                                     MessageRepository messageRepository,
                                     MagiVerdictRepository magiVerdictRepository,
                                     MagiSynthesisRepository magiSynthesisRepository) {
        this.slmClient = slmClient;
        this.complexityClassifier = complexityClassifier;
        this.magiCoordinator = magiCoordinator;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.magiVerdictRepository = magiVerdictRepository;
        this.magiSynthesisRepository = magiSynthesisRepository;
    }

    public Message handle(UUID userId, UUID conversationId, String prompt) {

        Conversation conversation = conversationId != null
                ? conversationRepository.findById(conversationId).orElseThrow()
                : conversationRepository.save(new Conversation(userId, null));

        messageRepository.save(new Message(conversation.getId(), "user", prompt));

        Message assistantMessage;

        if (complexityClassifier.isComplex(prompt)) {
            MagiOutcome outcome = magiCoordinator.run(prompt);

            String finalAnswer = outcome.consensusResult() == ConsensusResult.SPLIT
                    ? "MAGI did not reach consensus. See the differences breakdown."
                    : outcome.finalAnswer();

            assistantMessage = new Message(conversation.getId(), "assistant", finalAnswer);
            assistantMessage.setDecisionMode(DecisionMode.MAGI);
            assistantMessage.setConsensusResult(outcome.consensusResult());
            assistantMessage.setWinningUnit(outcome.winningUnit());
            messageRepository.save(assistantMessage);

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

        } else {
            String answer = slmClient.complete("You are a helpful assistant.", prompt, null);
            assistantMessage = new Message(conversation.getId(), "assistant", answer);
            assistantMessage.setDecisionMode(DecisionMode.DIRECT);
            messageRepository.save(assistantMessage);
        }

        return assistantMessage;
    }
}
