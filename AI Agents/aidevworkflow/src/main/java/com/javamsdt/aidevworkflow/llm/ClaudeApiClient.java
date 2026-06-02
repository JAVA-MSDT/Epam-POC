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
    private static final String BETA_PROMPT_CACHING = "prompt-caching-2024-07-31";
    private static final int MAX_TOKENS = 8192;
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
        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", prompt)
        );
        return buildAndExecute(content, false);
    }

    /**
     * Sends the codebase context as a cached content block followed by the task
     * instructions as a plain block. The {@code anthropic-beta: prompt-caching-2024-07-31}
     * header is added so the API server caches the first block for up to 5 minutes.
     * Subsequent calls with the same codebase snapshot hit the cache and skip
     * re-tokenizing it, reducing both latency and input-token cost.
     *
     * @param cacheableContext large, stable content (e.g., codebase snapshot); must be
     *                         at least 1 024 tokens for the cache to activate
     * @param taskPrompt       the variable task instructions
     * @return the model's text completion
     */
    @Override
    public String completePromptCached(String cacheableContext, String taskPrompt) {
        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", cacheableContext,
                        "cache_control", Map.of("type", "ephemeral")),
                Map.of("type", "text", "text", taskPrompt)
        );
        return buildAndExecute(content, true);
    }

    private String buildAndExecute(List<Map<String, Object>> contentBlocks, boolean useCache) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", MAX_TOKENS,
                    "messages", List.of(Map.of("role", "user", "content", contentBlocks))
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            Request.Builder rb = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json");

            if (useCache) {
                rb.addHeader("anthropic-beta", BETA_PROMPT_CACHING);
            }

            try (Response response = httpClient.newCall(rb.post(RequestBody.create(jsonBody, JSON_MEDIA)).build()).execute()) {
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
