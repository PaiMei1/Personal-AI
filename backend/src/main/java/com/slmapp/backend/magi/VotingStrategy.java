package com.slmapp.backend.magi;

import com.slmapp.backend.entity.ConsensusResult;
import com.slmapp.backend.entity.MagiUnit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VotingStrategy {

    // A self-vote only counts at full weight if the persona is genuinely confident in its own
    // answer. Below this threshold, a self-vote is halved so one mildly-confident persona can't
    // single-handedly block consensus - but a truly confident one still can.
    private static final double SELF_VOTE_CONFIDENCE_THRESHOLD = 0.8;
    private static final double SELF_VOTE_LOW_CONFIDENCE_WEIGHT = 0.5;
    private static final double FULL_WEIGHT = 1.0;

    public record Tally(ConsensusResult result, MagiUnit winner) {
    }

    public Tally tally(List<MagiVote> votes) {
        if (votes.isEmpty()) {
            return new Tally(ConsensusResult.SPLIT, null);
        }

        boolean rawUnanimous = votes.stream()
                .map(MagiVote::votedFor)
                .distinct()
                .count() == 1;

        Map<MagiUnit, Double> weightedSums = new HashMap<>();
        for (MagiVote vote : votes) {
            double weight = weightFor(vote);
            weightedSums.merge(vote.votedFor(), weight, Double::sum);
        }

        if (rawUnanimous) {
            return new Tally(ConsensusResult.UNANIMOUS, votes.get(0).votedFor());
        }

        double maxWeight = weightedSums.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        List<MagiUnit> leaders = weightedSums.entrySet().stream()
                .filter(e -> e.getValue() == maxWeight)
                .map(Map.Entry::getKey)
                .toList();

        if (leaders.size() == 1) {
            return new Tally(ConsensusResult.MAJORITY, leaders.get(0));
        }

        // Tied weighted sums - genuine, irreducible disagreement.
        return new Tally(ConsensusResult.SPLIT, null);
    }

    private double weightFor(MagiVote vote) {
        boolean isSelfVote = vote.voter() == vote.votedFor();
        if (isSelfVote && vote.confidence() < SELF_VOTE_CONFIDENCE_THRESHOLD) {
            return SELF_VOTE_LOW_CONFIDENCE_WEIGHT;
        }
        return FULL_WEIGHT;
    }
}