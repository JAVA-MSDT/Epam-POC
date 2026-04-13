package com.epam.llm;

import com.epam.constant.AppConstant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

/**
 * Client for interacting with Ollama LLM.
 * Handles connection, configuration, and generation requests.
 */
public class OllamaClient {
    private final ChatLanguageModel model;
    private final String modelName;
    
    /**
     * Creates an Ollama client with default settings.
     * Uses model on localhost: 11434
     */
    public OllamaClient() {
        this(AppConstant.OLLAMA_MODEL);
    }
    
    /**
     * Creates an Ollama client with a specified model.
     * 
     * @param modelName Name of the Ollama model (e.g., "deepseek-coder:6.7b", "llama3:8b")
     */
    public OllamaClient(String modelName) {
        this.modelName = modelName;
        this.model = OllamaChatModel.builder()
                .baseUrl(AppConstant.OLLAMA_BASE_URL)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .temperature(0.7)
                .build();
    }
    
    /**
     * Generates a response from the LLM based on the prompt.
     * 
     * @param prompt The prompt to send to the LLM
     * @return Generated response text
     * @throws OllamaException if Ollama is not available or generation fails
     */
    public String generate(String prompt) {
        try {
            System.out.println("🤖 Generating response with " + modelName + "...");
            String response = model.generate(prompt);
            System.out.println("✅ Response generated successfully");
            return response;
        } catch (Exception e) {
            throw new OllamaException(
                "Failed to generate response from Ollama. " +
                "Please ensure Ollama is running (ollama serve) and model is downloaded (ollama pull " + modelName + ")",
                e
            );
        }
    }
    
    /**
     * Exception thrown when Ollama operations fail.
     */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
