package com.slmapp.backend.magi;

import com.slmapp.backend.entity.ConsensusResult;
import com.slmapp.backend.entity.MagiUnit;

import java.util.List;

public record MagiOutcome(
        ConsensusResult consensusResult,
        MagiUnit winningUnit,
        String finalAnswer,
        List<MagiCandidateAnswer> candidates,
        List<MagiVote> votes,
        String commonGroundJson,
        String differencesJson
) {
}
