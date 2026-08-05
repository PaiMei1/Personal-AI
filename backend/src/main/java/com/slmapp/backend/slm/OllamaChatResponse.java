package com.slmapp.backend.slm.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ollama's response includes several timing/metadata fields we don't need yet
// (total_duration, eval_count, etc) - ignore unknowns rather than mapping all of them.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(OllamaChatMessage message, boolean done) {
}