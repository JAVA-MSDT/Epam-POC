package com.epam.feedback;

import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates actionable feedback by combining static analysis findings with knowledge base entries.
 * This is the "Generation" part of the RAG (Retrieval-Augmented Generation) pipeline.
 * Uses template-based generation for flexible output formatting.
 */
public class FeedbackGenerator {
    private static final String TEMPLATE_PATH = "src/main/resources/templates/feedback_template.txt";
    private String template;

    public FeedbackGenerator() {
        loadTemplate();
    }

    /**
     * Loads the feedback template from a file with fallback to the hardcoded template.
     */
    private void loadTemplate() {
        try {
            template = Files.readString(Paths.get(TEMPLATE_PATH));
        } catch (IOException e) {
            // Fallback to hardcoded template if a file not found
            template = """
                    === CODE REVIEW FEEDBACK ===
                    Issue Detected: ${issue}
                    Location: ${details}

                    Knowledge Base Guidance:
                    Title: ${title}
                    Type: ${type}
                    Description: ${description}

                    Example/Suggestion: ${example}

                    Reference: ${reference}
                    =============================
                    """;
        }
    }

    /**
     * Generates formatted feedback using a template with variable substitution.
     * 
     * @param finding The static analysis finding (issue detected)
     * @param entry The relevant knowledge base entry (best practice/guidance)
     * @return Formatted feedback string using template
     */
    public String generateFeedback(AnalysisFinding finding, KnowledgeEntry entry) {
        return template
            .replace("${issue}", finding.issue())
            .replace("${details}", finding.details())
            .replace("${title}", entry.getTitle())
            .replace("${type}", entry.getType())
            .replace("${description}", entry.getDescription())
            .replace("${example}", entry.getExample() != null ? entry.getExample() : "N/A")
            .replace("${reference}", entry.getReference() != null ? entry.getReference() : "N/A");
    }

    /**
     * Generates simple feedback when no knowledge base entry is found for a finding.
     * 
     * @param finding The static analysis finding without matching knowledge
     * @param availableTopics List of available knowledge base topics for suggestions
     * @return Basic feedback string for the finding with knowledge base context
     */
    public String generateBasicFeedback(AnalysisFinding finding, List<String> availableTopics) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("=== CODE REVIEW FEEDBACK ===\n");
        feedback.append("Issue Detected: ").append(finding.issue()).append("\n");
        feedback.append("Location: ").append(finding.details()).append("\n\n");
        
        feedback.append("Knowledge Base Status:\n");
        feedback.append("No specific guidance found for '").append(finding.issue()).append("'\n");
        
        if (!availableTopics.isEmpty()) {
            feedback.append("\nAvailable knowledge base topics:\n");
            for (String topic : availableTopics.subList(0, Math.min(3, availableTopics.size()))) {
                feedback.append("  - ").append(topic).append("\n");
            }
            if (availableTopics.size() > 3) {
                feedback.append("  ... and ").append(availableTopics.size() - 3).append(" more topics\n");
            }
        }
        
        feedback.append("\nSuggestion: Consider adding this pattern to the knowledge base for future reference.\n");
        feedback.append("=============================\n");
        
        return feedback.toString();
    }
}