package com.slmapp.backend.slm;

public interface SlmClient {

    /**
     * Single-turn request/response. No streaming in v1 - the WebSocket layer
     * will add a streaming variant later without changing this contract.
     * modelId may be null, in which case the implementation's default model is used.
     */
    String complete(String systemPrompt, String userPrompt, String modelId);
}