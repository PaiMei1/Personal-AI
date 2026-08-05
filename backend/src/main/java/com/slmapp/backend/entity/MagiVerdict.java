package com.slmapp.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "magi_verdicts")
public class MagiVerdict {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MagiUnit unit;

    @Column(nullable = false)
    private int round;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "voted_for_unit")
    private MagiUnit votedForUnit;

    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MagiVerdict() {
    }

    public MagiVerdict(UUID messageId, MagiUnit unit, int round, String answerText,
                        MagiUnit votedForUnit, Double confidence) {
        this.messageId = messageId;
        this.unit = unit;
        this.round = round;
        this.answerText = answerText;
        this.votedForUnit = votedForUnit;
        this.confidence = confidence;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public MagiUnit getUnit() {
        return unit;
    }

    public int getRound() {
        return round;
    }

    public String getAnswerText() {
        return answerText;
    }

    public MagiUnit getVotedForUnit() {
        return votedForUnit;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
