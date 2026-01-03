/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.epam.ragcodeassistant.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final OllamaChatModel model;

    public LlmService() {
        this.model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("mistral")
                .build();
    }

    public String generateSuggestion(String prompt, String code) {
        String fullPrompt = prompt + "\n\nCode:\n" + code;
        return model.chat(fullPrompt);
    }
}