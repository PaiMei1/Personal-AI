package com.slmapp.backend.magi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SynthesisEngine {

    private static final String SYSTEM_PROMPT = """
            You compare three independent answers to the same question and produce a structured comparison.
            Respond with ONLY valid JSON, no other text, in this exact shape:
            {"commonGround": ["..."], "differences": [{"point": "...", "supportedBy": ["MELCHIOR"], "opposedBy": ["CASPER"]}]}
            """;

    private final SlmClient slmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SynthesisEngine(SlmClient slmClient) {
        this.slmClient = slmClient;
    }

    public record Synthesis(String commonGroundJson, String differencesJson) {
    }

    public Synthesis synthesize(List<MagiCandidateAnswer> candidates) {
        StringBuilder userPrompt = new StringBuilder("Here are three independent answers:\n\n");
        for (MagiCandidateAnswer c : candidates) {
            userPrompt.append(c.unit()).append(": ").append(c.answer()).append("\n\n");
        }

        String raw = slmClient.complete(SYSTEM_PROMPT, userPrompt.toString(), null);
        String json = extractJson(raw);

        try {
            JsonNode node = objectMapper.readTree(json);
            String commonGround = node.has("commonGround") ? node.get("commonGround").toString() : "[]";
            String differences = node.has("differences") ? node.get("differences").toString() : "[]";
            return new Synthesis(commonGround, differences);
        } catch (Exception e) {
            // Model didn't return valid JSON - fall back to empty rather than failing the request.
            // Raw candidate answers are still persisted separately via MagiVerdict rows.
            return new Synthesis("[]", "[]");
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
