package com.epam.model;

/**
 * Represents a static analysis finding (issue) discovered by tools like Checkstyle or PMD.
 * Contains the issue description and additional details about the finding.
 */
public record AnalysisFinding(String issue, String details) {
    /**
     * Creates a new analysis finding.
     *
     * @param issue   The main issue or rule violation description
     * @param details Additional details about the finding (file, line, etc.)
     */
    public AnalysisFinding {
    }

    /**
     * Gets the main issue description.
     *
     * @return The issue description
     */
    @Override
    public String issue() {
        return issue;
    }

    /**
     * Gets additional details about the finding.
     *
     * @return The finding details
     */
    @Override
    public String details() {
        return details;
    }
}