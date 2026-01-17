package com.epam.feedback;

import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;

/**
 * Generates actionable feedback by combining static analysis findings with knowledge base entries.
 * This is the "Generation" part of the RAG (Retrieval-Augmented Generation) pipeline.
 */
public class FeedbackGenerator {

    /**
     * Generates formatted feedback by combining an analysis finding with relevant knowledge.
     * 
     * @param finding The static analysis finding (issue detected)
     * @param entry The relevant knowledge base entry (best practice/guidance)
     * @return Formatted feedback string combining finding and knowledge
     */
    public String generateFeedback(AnalysisFinding finding, KnowledgeEntry entry) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("=== CODE REVIEW FEEDBACK ===\n");
        feedback.append("Issue Detected: ").append(finding.getIssue()).append("\n");
        feedback.append("Location: ").append(finding.getDetails()).append("\n\n");
        
        feedback.append("Knowledge Base Guidance:\n");
        feedback.append("Title: ").append(entry.getTitle()).append("\n");
        feedback.append("Type: ").append(entry.getType()).append("\n");
        feedback.append("Description: ").append(entry.getDescription()).append("\n\n");
        
        if (entry.getExample() != null && !entry.getExample().trim().isEmpty()) {
            feedback.append("Example/Suggestion: ").append(entry.getExample()).append("\n\n");
        }
        
        if (entry.getReference() != null && !entry.getReference().trim().isEmpty()) {
            feedback.append("Reference: ").append(entry.getReference()).append("\n");
        }
        
        feedback.append("=============================\n");
        
        return feedback.toString();
    }

    /**
     * Generates simple feedback when no knowledge base entry is found for a finding.
     * 
     * @param finding The static analysis finding without matching knowledge
     * @return Basic feedback string for the finding
     */
    public String generateBasicFeedback(AnalysisFinding finding) {
        return String.format(
                """
                        === CODE REVIEW FEEDBACK ===
                        Issue Detected: %s
                        Location: %s
                        Note: No specific guidance found in knowledge base.
                        =============================
                        """,
            finding.getIssue(),
            finding.getDetails()
        );
    }
}