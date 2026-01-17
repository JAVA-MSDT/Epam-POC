package com.epam.model;

/**
 * Represents a static analysis finding (issue) discovered by tools like Checkstyle or PMD.
 * Contains the issue description and additional details about the finding.
 */
public class AnalysisFinding {
    private final String issue;
    private final String details;

    /**
     * Creates a new analysis finding.
     * 
     * @param issue The main issue or rule violation description
     * @param details Additional details about the finding (file, line, etc.)
     */
    public AnalysisFinding(String issue, String details) {
        this.issue = issue;
        this.details = details;
    }

    /**
     * Gets the main issue description.
     * 
     * @return The issue description
     */
    public String getIssue() { 
        return issue; 
    }

    /**
     * Gets additional details about the finding.
     * 
     * @return The finding details
     */
    public String getDetails() { 
        return details; 
    }

    @Override
    public String toString() {
        return "AnalysisFinding{issue='" + issue + "', details='" + details + "'}";
    }
}