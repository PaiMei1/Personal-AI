package com.slmapp.backend.magi.events;

import com.slmapp.backend.entity.ConsensusResult;
import com.slmapp.backend.entity.MagiUnit;

import java.util.UUID;

public record MagiDecisionEvent(UUID messageId, ConsensusResult result, MagiUnit winningUnit) {
}
