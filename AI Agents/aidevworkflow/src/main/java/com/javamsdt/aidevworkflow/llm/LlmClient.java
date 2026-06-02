package com.javamsdt.aidevworkflow.llm;

/**
 * Pluggable LLM abstraction. Agents and the orchestrator depend only on this
 * interface — swap Claude, OpenAI, or any other provider by changing the
 * concrete implementation injected at construction time.
 */
public interface LlmClient {

    /**
     * Sends a fully formed prompt to the underlying LLM and returns the
     * raw text response.
     *
     * @param prompt the complete prompt string
     * @return the model's text completion
     */
    String completePrompt(String prompt);
}
