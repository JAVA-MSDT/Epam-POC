package com.javamsdt.aidevworkflow.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OllamaApiClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaApiClient(String baseUrl) {
        this(baseUrl, DEFAULT_MODEL);
    }

    public OllamaApiClient(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convenience factory — reads base URL from OLLAMA_BASE_URL env var,
     * defaulting to http://localhost:11434 if not set.
     */
    public static OllamaApiClient fromEnv() {
        String url = System.getenv("OLLAMA_BASE_URL");
        if (url == null || url.isBlank()) {
            url = DEFAULT_BASE_URL;
        }
        return new OllamaApiClient(url);
    }

    @Override
    public String completePrompt(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/chat")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Ollama API error " + response.code() + ": " + responseBody);
                }
                Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
                Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
                return (String) message.get("content");
            }
        } catch (IOException e) {
            throw new RuntimeException("Ollama API call failed: " + e.getMessage(), e);
        }
    }
}
