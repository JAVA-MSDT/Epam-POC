package com.javamsdt.aidevworkflow.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeApiClientTest {

    @Test
    void constructorShouldAcceptAnyApiKey() {
        assertDoesNotThrow(() -> new ClaudeApiClient("test-key"));
    }

    @Test
    void constructorShouldAcceptCustomModel() {
        assertDoesNotThrow(() -> new ClaudeApiClient("test-key", "claude-haiku-4-5-20251001"));
    }

    /**
     * Integration test — only runs when ANTHROPIC_API_KEY is set.
     * Run manually: export ANTHROPIC_API_KEY=your_key && mvn test
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void completePromptShouldReturnNonEmptyResponseFromRealApi() {
        ClaudeApiClient client = ClaudeApiClient.fromEnv();
        String response = client.completePrompt("Reply with exactly the word: PONG");
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertTrue(response.toUpperCase().contains("PONG"),
                "Expected response to contain PONG, got: " + response);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void fromEnvShouldCreateClientFromEnvironmentVariable() {
        assertDoesNotThrow(ClaudeApiClient::fromEnv);
    }
}
