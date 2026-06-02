package com.javamsdt.aidevworkflow.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LlmClient implementation that calls the OpenAI Chat Completions API.
 * API key is read from the OPENAI_API_KEY environment variable by default.
 */
public class OpenAiApiClient implements LlmClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiApiClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public OpenAiApiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convenience factory — reads the API key from OPENAI_API_KEY env var.
     */
    public static OpenAiApiClient fromEnv() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Environment variable OPENAI_API_KEY is not set.");
        }
        return new OpenAiApiClient(key);
    }

    @Override
    public String completePrompt(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API error " + response.code() + ": " + responseBody);
                }
                Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
                List<?> choices = (List<?>) responseMap.get("choices");
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                return (String) message.get("content");
            }
        } catch (IOException e) {
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }
}
