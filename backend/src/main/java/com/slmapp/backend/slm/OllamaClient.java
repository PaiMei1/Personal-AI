package com.slmapp.backend.slm.ollama;

import com.slmapp.backend.exception.ApplicationException;
import com.slmapp.backend.slm.OllamaProperties;
import com.slmapp.backend.slm.SlmClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class OllamaClient implements SlmClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, String modelId) {
        String model = (modelId != null && !modelId.isBlank()) ? modelId : properties.defaultModel();

        OllamaChatRequest request = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaChatMessage("system", systemPrompt),
                        new OllamaChatMessage("user", userPrompt)
                ),
                false,
                false
        );

        try {
            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null || response.message() == null) {
                throw new ApplicationException("Ollama returned an empty response", HttpStatus.BAD_GATEWAY);
            }

            return response.message().content();

        } catch (RestClientException ex) {
            throw new ApplicationException(
                    "Failed to reach Ollama at " + properties.baseUrl() + ": " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
}