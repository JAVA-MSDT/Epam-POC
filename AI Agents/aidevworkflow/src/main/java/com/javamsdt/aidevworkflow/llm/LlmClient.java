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

    /**
     * Sends a prompt split into a large, stable context block and a variable
     * task block. The context block (e.g., a codebase snapshot) is marked for
     * server-side caching so subsequent calls with the same context skip
     * re-tokenizing it, reducing latency and cost.
     * <p>
     * Default implementation: concatenates both parts and delegates to
     * {@link #completePrompt}. Override in implementations that support
     * prompt caching (e.g., Anthropic Claude).
     *
     * @param cacheableContext large, stable content to cache (e.g., codebase snapshot);
     *                         sent first so the LLM reads it before the task instructions
     * @param taskPrompt       the variable instructions; should reference the codebase
     *                         with a note like "(see codebase context above)"
     * @return the model's text completion
     */
    default String completePromptCached(String cacheableContext, String taskPrompt) {
        return completePrompt(cacheableContext + "\n\n" + taskPrompt);
    }
}
