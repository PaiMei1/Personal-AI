package com.slmapp.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "magi_syntheses")
public class MagiSynthesis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "common_ground", columnDefinition = "TEXT")
    private String commonGroundJson;

    @Column(name = "differences", columnDefinition = "TEXT")
    private String differencesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MagiSynthesis() {
    }

    public MagiSynthesis(UUID messageId, String commonGroundJson, String differencesJson) {
        this.messageId = messageId;
        this.commonGroundJson = commonGroundJson;
        this.differencesJson = differencesJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getCommonGroundJson() {
        return commonGroundJson;
    }

    public String getDifferencesJson() {
        return differencesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
