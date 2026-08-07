package com.slmapp.backend.slm.ollama;

import java.util.List;

public record OllamaChatRequest(String model, List<OllamaChatMessage> messages, boolean stream) {
}