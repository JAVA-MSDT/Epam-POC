package com.epam.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

@Service
@Slf4j
public class LlmService {
    
    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.model:mistral}")
    private String model;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String generateCodeExplanation(String code) {
        log.info("Generating code explanation");
        
        String prompt = "Explain the following code in detail:\n\n" + code;
        return callOllama(prompt);
    }
    
    public String generateRefactoringSuggestion(String code, String context) {
        log.info("Generating refactoring suggestion");
        
        String prompt = String.format(
            "Analyze the following code and provide refactoring suggestions to improve code quality, " +
            "performance, and maintainability. Context: %s\n\nCode:\n%s", 
            context, code
        );
        return callOllama(prompt);
    }
    
    public String generateCodeReview(String code) {
        log.info("Generating code review");
        
        String prompt = "Perform a code review on the following code. " +
                       "Identify potential issues, bugs, security vulnerabilities, " +
                       "and suggest improvements:\n\n" + code;
        return callOllama(prompt);
    }
    
    private String callOllama(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                ollamaUrl + "/api/generate", 
                request, 
                Map.class
            );
            
            return response != null ? (String) response.get("response") : "No response from LLM";
            
        } catch (Exception e) {
            log.error("Error calling Ollama: {}", e.getMessage());
            return "Error: Unable to connect to local LLM. Please ensure Ollama is running.";
        }
    }
}