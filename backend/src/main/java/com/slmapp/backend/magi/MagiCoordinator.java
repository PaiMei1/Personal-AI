package com.slmapp.backend.magi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmapp.backend.entity.MagiUnit;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MagiCoordinator {

    private static final List<String> LABELS = List.of("A", "B", "C");

    private final SlmClient slmClient;
    private final VotingStrategy votingStrategy;
    private final SynthesisEngine synthesisEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Shuffle candidates behind anonymous labels (A/B/C) once per run, shared across all
        // voters. This is what removes self-bias: a persona votes on anonymized answers and
        // does not know which one is its own, so a self-vote can only happen because the
        // answer is genuinely judged best - not because of identity.
        List<MagiUnit> shuffledUnits = new ArrayList<>(List.of(MagiUnit.values()));
        Collections.shuffle(shuffledUnits);

        Map<String, MagiUnit> labelToUnit = new HashMap<>();
        Map<MagiUnit, String> unitToLabel = new HashMap<>();
        for (int i = 0; i < shuffledUnits.size(); i++) {
            labelToUnit.put(LABELS.get(i), shuffledUnits.get(i));
            unitToLabel.put(shuffledUnits.get(i), LABELS.get(i));
        }

        String anonymizedBlock = buildAnonymizedBlock(candidates, unitToLabel);

        List<MagiVote> votes = new ArrayList<>();
        for (MagiPersona persona : MagiPersona.values()) {
            String voteSystemPrompt = persona.systemPrompt() + """

                    You will be shown three candidate answers labeled A, B, and C to the same question.
                    You do not know which answer is your own. Judge each purely on accuracy,
                    completeness, and quality of reasoning.

                    Respond with ONLY valid JSON, no other text, in this exact shape:
                    {"votedLabel": "A", "confidence": 0.8, "justification": "one short sentence"}

                    votedLabel must be exactly one of: A, B, C.
                    confidence must be a number between 0.0 and 1.0.
                    """;
            String voteUserPrompt = "Question: " + userPrompt + "\n\n" + anonymizedBlock;

            String rawVote = slmClient.complete(voteSystemPrompt, voteUserPrompt, null);
            MagiVote vote = parseVote(persona.unit(), rawVote, labelToUnit);
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

    private String buildAnonymizedBlock(List<MagiCandidateAnswer> candidates, Map<MagiUnit, String> unitToLabel) {
        StringBuilder sb = new StringBuilder();
        for (MagiCandidateAnswer c : candidates) {
            sb.append(unitToLabel.get(c.unit())).append(": ").append(c.answer()).append("\n\n");
        }
        return sb.toString();
    }

    private MagiVote parseVote(MagiUnit voter, String raw, Map<String, MagiUnit> labelToUnit) {
        String json = extractJson(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            String label = node.path("votedLabel").asText(null);
            if (label == null) {
                return null;
            }
            MagiUnit votedFor = labelToUnit.get(label.trim().toUpperCase());
            if (votedFor == null) {
                return null;
            }
            double confidence = Math.max(0.0, Math.min(1.0, node.path("confidence").asDouble(0.5)));
            String justification = node.path("justification").asText("");
            return new MagiVote(voter, votedFor, confidence, justification);
        } catch (Exception e) {
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