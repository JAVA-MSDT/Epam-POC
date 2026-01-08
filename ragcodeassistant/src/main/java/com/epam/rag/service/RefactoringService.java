package com.epam.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefactoringService {
    
    private final CodeAnalysisService codeAnalysisService;
    private final LlmService llmService;
    private final VectorDbService vectorDbService;
    private final EmbeddingService embeddingService;
    
    public Map<String, Object> generateRefactoringSuggestions(String code, String language) {
        log.info("Generating refactoring suggestions for {} code", language);
        
        Map<String, Object> result = new HashMap<>();
        
        // 1. Static code analysis
        Map<String, Object> analysisResult = codeAnalysisService.analyzeCode(code, language);
        result.put("staticAnalysis", analysisResult);
        
        // 2. Find similar code patterns for context
        String context = findSimilarCodeContext(code);
        result.put("similarPatterns", context);
        
        // 3. Get LLM-based suggestions
        try {
            String llmSuggestions = llmService.generateRefactoringSuggestion(code, context);
            result.put("aiSuggestions", llmSuggestions);
        } catch (Exception e) {
            log.error("Error getting LLM suggestions: {}", e.getMessage());
            result.put("aiSuggestions", "Unable to generate AI suggestions: " + e.getMessage());
        }
        
        result.put("language", language);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    public Map<String, Object> performCodeReview(String code, String language) {
        log.info("Performing code review for {} code", language);
        
        Map<String, Object> result = new HashMap<>();
        
        // 1. Static analysis
        Map<String, Object> analysisResult = codeAnalysisService.analyzeCode(code, language);
        result.put("staticAnalysis", analysisResult);
        
        // 2. LLM-based review
        try {
            String llmReview = llmService.generateCodeReview(code);
            result.put("detailedReview", llmReview);
        } catch (Exception e) {
            log.error("Error getting LLM review: {}", e.getMessage());
            result.put("detailedReview", "Unable to generate detailed review: " + e.getMessage());
        }
        
        result.put("language", language);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    private String findSimilarCodeContext(String code) {
        try {
            // Generate embedding for the input code
            float[] codeEmbedding = embeddingService.generateCodeEmbedding(code, "java");
            
            // Search for similar code in the vector database
            var similarDocs = vectorDbService.searchSimilar(codeEmbedding, 3, 0.7);
            
            if (similarDocs.isEmpty()) {
                return "No similar code patterns found in the codebase.";
            }
            
            StringBuilder context = new StringBuilder("Similar code patterns found:\n");
            similarDocs.forEach(doc -> 
                context.append("- ").append(doc.getFileName())
                       .append(" (Class: ").append(doc.getClassName()).append(")\n")
            );
            
            return context.toString();
            
        } catch (Exception e) {
            log.error("Error finding similar code context: {}", e.getMessage());
            return "Unable to find similar code patterns.";
        }
    }
    
    public String explainCode(String code) {
        log.info("Generating code explanation");
        try {
            return llmService.generateCodeExplanation(code);
        } catch (Exception e) {
            log.error("Error generating code explanation: {}", e.getMessage());
            return "Unable to generate code explanation: " + e.getMessage();
        }
    }
}