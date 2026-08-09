package com.slmapp.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model_used")
    private String modelUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_mode", nullable = false)
    private DecisionMode decisionMode = DecisionMode.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(name = "consensus_result")
    private ConsensusResult consensusResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "winning_unit")
    private MagiUnit winningUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.COMPLETE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Message() {
    }

    public Message(UUID conversationId, String role, String content) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public DecisionMode getDecisionMode() {
        return decisionMode;
    }

    public void setDecisionMode(DecisionMode decisionMode) {
        this.decisionMode = decisionMode;
    }

    public ConsensusResult getConsensusResult() {
        return consensusResult;
    }

    public void setConsensusResult(ConsensusResult consensusResult) {
        this.consensusResult = consensusResult;
    }

    public MagiUnit getWinningUnit() {
        return winningUnit;
    }

    public void setWinningUnit(MagiUnit winningUnit) {
        this.winningUnit = winningUnit;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
