package com.slmapp.backend.magi;

import com.slmapp.backend.entity.MagiUnit;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MagiCoordinator {

    private final SlmClient slmClient;
    private final VotingStrategy votingStrategy;
    private final SynthesisEngine synthesisEngine;

    public MagiCoordinator(SlmClient slmClient, VotingStrategy votingStrategy, SynthesisEngine synthesisEngine) {
        this.slmClient = slmClient;
        this.votingStrategy = votingStrategy;
        this.synthesisEngine = synthesisEngine;
    }

    public MagiOutcome run(String userPrompt) {

        List<MagiCandidateAnswer> candidates = new ArrayList<>();
        for (MagiPersona persona : MagiPersona.values()) {
            String answer = slmClient.complete(persona.systemPrompt(), userPrompt, null);
            candidates.add(new MagiCandidateAnswer(persona.unit(), answer));
        }

        String candidateBlock = buildCandidateBlock(candidates);
        List<MagiVote> votes = new ArrayList<>();
        for (MagiPersona persona : MagiPersona.values()) {

            List<MagiUnit> otherUnits = new ArrayList<>();
            for (MagiUnit unit : MagiUnit.values()) {
                if (unit != persona.unit()) {
                    otherUnits.add(unit);
                }
            }

            String voteSystemPrompt = persona.systemPrompt() + """

                    You will be shown three candidate answers labeled MELCHIOR, BALTHASAR, and CASPER
                    to the same question. Your own answer is """ + persona.unit() + """
                     - you may NOT vote for your own answer, even if you think it is best.
                    Choose whichever of the OTHER TWO answers is more complete, accurate, and well-reasoned.
                    Respond with ONLY one word: """ + otherUnits.get(0) + " or " + otherUnits.get(1) + """
                    """;
            String voteUserPrompt = "Question: " + userPrompt + "\n\n" + candidateBlock;

            String rawVote = slmClient.complete(voteSystemPrompt, voteUserPrompt, null);
            MagiUnit votedFor = parseVote(rawVote, otherUnits);
            if (votedFor != null) {
                votes.add(new MagiVote(persona.unit(), votedFor));
            }
        }

        VotingStrategy.Tally tally = votingStrategy.tally(votes);

        if (tally.result() == com.slmapp.backend.entity.ConsensusResult.UNANIMOUS
                || tally.result() == com.slmapp.backend.entity.ConsensusResult.MAJORITY) {
            String winningAnswer = candidates.stream()
                    .filter(c -> c.unit() == tally.winner())
                    .findFirst()
                    .map(MagiCandidateAnswer::answer)
                    .orElse("");
            return new MagiOutcome(tally.result(), tally.winner(), winningAnswer, candidates, votes, null, null);
        }

        SynthesisEngine.Synthesis synthesis = synthesisEngine.synthesize(candidates);
        return new MagiOutcome(tally.result(), null, null, candidates, votes,
                synthesis.commonGroundJson(), synthesis.differencesJson());
    }

    private String buildCandidateBlock(List<MagiCandidateAnswer> candidates) {
        StringBuilder sb = new StringBuilder();
        for (MagiCandidateAnswer c : candidates) {
            sb.append(c.unit()).append(": ").append(c.answer()).append("\n\n");
        }
        return sb.toString();
    }

    private MagiUnit parseVote(String raw, List<MagiUnit> allowedUnits) {
        String cleaned = raw.trim().toUpperCase();
        for (MagiUnit unit : allowedUnits) {
            if (cleaned.contains(unit.name())) {
                return unit;
            }
        }
        return null;
    }
}