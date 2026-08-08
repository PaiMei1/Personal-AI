package com.slmapp.backend.magi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String VOTE_JSON_INSTRUCTIONS = """

            You will be shown three candidate answers labeled MELCHIOR, BALTHASAR, and CASPER
            to the same question, including your own. You may vote for your own answer if you are
            genuinely confident it is best - do not default to it out of habit, and do not avoid it
            out of false modesty either. Rate your confidence honestly.

            Respond with ONLY valid JSON, no other text, in this exact shape:
            {"votedFor": "MELCHIOR", "confidence": 0.85, "justification": "one short sentence"}

            votedFor must be exactly one of: MELCHIOR, BALTHASAR, CASPER.
            confidence must be a number between 0.0 and 1.0.
            """;

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
            String voteSystemPrompt = persona.systemPrompt() + VOTE_JSON_INSTRUCTIONS;
            String voteUserPrompt = "Question: " + userPrompt + "\n\n" + candidateBlock;

            String rawVote = slmClient.complete(voteSystemPrompt, voteUserPrompt, null);
            MagiVote vote = parseVote(persona.unit(), rawVote);
            if (vote != null) {
                votes.add(vote);
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

    private MagiVote parseVote(MagiUnit voter, String raw) {
        String json = extractJson(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            String votedForStr = node.path("votedFor").asText(null);
            if (votedForStr == null) {
                return null;
            }
            MagiUnit votedFor = MagiUnit.valueOf(votedForStr.trim().toUpperCase());
            double confidence = node.path("confidence").asDouble(0.5);
            confidence = Math.max(0.0, Math.min(1.0, confidence));
            String justification = node.path("justification").asText("");
            return new MagiVote(voter, votedFor, confidence, justification);
        } catch (Exception e) {
            // Model didn't return parseable JSON or a valid unit name - drop this vote
            // rather than guess. VotingStrategy handles a reduced vote count gracefully.
            return null;
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "{}";
    }
}