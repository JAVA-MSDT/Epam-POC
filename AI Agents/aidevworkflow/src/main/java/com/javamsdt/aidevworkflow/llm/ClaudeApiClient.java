package com.javamsdt.aidevworkflow.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LlmClient implementation that calls the Anthropic Claude Messages API.
 * Configure the model via the constructor or the MODEL constant.
 * API key is read from the ANTHROPIC_API_KEY environment variable by default.
 */
public class ClaudeApiClient implements LlmClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClaudeApiClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public ClaudeApiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convenience factory — reads the API key from ANTHROPIC_API_KEY env var.
     */
    public static ClaudeApiClient fromEnv() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Environment variable ANTHROPIC_API_KEY is not set.");
        }
        return new ClaudeApiClient(key);
    }

    @Override
    public String completePrompt(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", MAX_TOKENS,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Claude API error " + response.code() + ": " + responseBody);
                }
                Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
                List<?> content = (List<?>) responseMap.get("content");
                Map<?, ?> firstContent = (Map<?, ?>) content.get(0);
                return (String) firstContent.get("text");
            }
        } catch (IOException e) {
            throw new RuntimeException("Claude API call failed: " + e.getMessage(), e);
        }
    }
}
