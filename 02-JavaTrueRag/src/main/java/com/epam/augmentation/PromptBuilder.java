package com.epam.augmentation;

import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;

import java.util.List;

/**
 * Builds prompts for the LLM by combining user queries, code context, and knowledge base entries.
 * This is the Augmentation part of RAG-enriching prompts with retrieved context.
 */
public class PromptBuilder {
    
    /**
     * Builds a prompt for explaining code issues with knowledge base context.
     * 
     * @param userQuery The user's question or intent
     * @param findings List of code issues found
     * @param knowledgeEntries Relevant knowledge base entries
     * @param codeSnippet Optional code snippet for context
     * @return Formatted prompt for the LLM
     */
    public String buildPrompt(
        String userQuery,
        List<AnalysisFinding> findings,
        List<KnowledgeEntry> knowledgeEntries,
        String codeSnippet
    ) {
        StringBuilder prompt = new StringBuilder();
        
        // System instruction
        prompt.append("You are an expert Java code reviewer. ");
        prompt.append("Provide clear, educational, and actionable feedback.\n\n");
        
        // User query
        prompt.append("USER REQUEST:\n");
        prompt.append(userQuery).append("\n\n");
        
        // Code context
        if (codeSnippet != null && !codeSnippet.isEmpty()) {
            prompt.append("CODE BEING ANALYZED:\n");
            prompt.append("```java\n");
            prompt.append(truncateCode(codeSnippet, 500));
            prompt.append("\n```\n\n");
        }
        
        // Issues found
        if (!findings.isEmpty()) {
            prompt.append("ISSUES DETECTED:\n");
            for (int i = 0; i < Math.min(findings.size(), 5); i++) {
                AnalysisFinding finding = findings.get(i);
                prompt.append(String.format("%d. %s\n   Location: %s\n", 
                    i + 1, finding.issue(), finding.details()));
            }
            prompt.append("\n");
        }
        
        // Knowledge base context
        if (!knowledgeEntries.isEmpty()) {
            prompt.append("RELEVANT BEST PRACTICES:\n");
            for (KnowledgeEntry entry : knowledgeEntries) {
                prompt.append(String.format("- %s (%s)\n", entry.getTitle(), entry.getType()));
                prompt.append(String.format("  %s\n", entry.getDescription()));
                if (entry.getExample() != null) {
                    prompt.append(String.format("  Example: %s\n", entry.getExample()));
                }
            }
            prompt.append("\n");
        }
        
        // Instructions
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("Provide a helpful response that:\n");
        prompt.append("1. Explains why the detected issues matter\n");
        prompt.append("2. References the best practices from the knowledge base\n");
        prompt.append("3. Suggests concrete improvements\n");
        prompt.append("4. Uses a friendly, educational tone\n");
        
        return prompt.toString();
    }
    
    /**
     * Builds a simple prompt for general code questions.
     * 
     * @param userQuery The user's question
     * @param codeSnippet Code to analyze
     * @return Formatted prompt
     */
    public String buildSimplePrompt(String userQuery, String codeSnippet) {
        return String.format("""
            You are an expert Java code reviewer.
            
            USER QUESTION:
            %s
            
            CODE:
            ```java
            %s
            ```
            
            Provide a clear, helpful answer.
            """,
            userQuery,
            truncateCode(codeSnippet, 1000)
        );
    }
    
    /**
     * Truncates code to a maximum length to avoid token limits.
     */
    private String truncateCode(String code, int maxLength) {
        if (code.length() <= maxLength) {
            return code;
        }
        return code.substring(0, maxLength) + "\n... (truncated)";
    }
}
