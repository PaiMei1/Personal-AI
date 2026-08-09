package com.slmapp.backend.magi;

import com.slmapp.backend.entity.ConsensusResult;
import com.slmapp.backend.entity.MagiUnit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VotingStrategy {

    public record Tally(ConsensusResult result, MagiUnit winner) {
    }

    public Tally tally(List<MagiVote> votes) {
        if (votes.isEmpty()) {
            return new Tally(ConsensusResult.SPLIT, null);
        }

        Map<MagiUnit, Long> counts = votes.stream()
                .collect(Collectors.groupingBy(MagiVote::votedFor, Collectors.counting()));

        long maxVotes = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<MagiUnit> leaders = counts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        if (leaders.size() == 1 && maxVotes == votes.size()) {
            return new Tally(ConsensusResult.UNANIMOUS, leaders.get(0));
        }
        if (leaders.size() == 1) {
            return new Tally(ConsensusResult.MAJORITY, leaders.get(0));
        }
        return new Tally(ConsensusResult.SPLIT, null);
    }
}