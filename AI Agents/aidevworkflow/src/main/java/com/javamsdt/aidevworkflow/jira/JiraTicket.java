package com.javamsdt.aidevworkflow.jira;

/**
 * Structured representation of a Jira issue, ready for injection into an LLM prompt.
 */
public record JiraTicket(
        String issueKey,
        String summary,
        String description,
        String issueType,
        String status,
        String assignee,
        String reporter,
        String priority,
        String labels,
        String comments
) {

    /** Formats the ticket as a structured block for use in an LLM prompt. */
    public String toPromptText() {
        return """
                Ticket ID:   %s
                Summary:     %s
                Type:        %s
                Status:      %s
                Priority:    %s
                Assignee:    %s
                Reporter:    %s
                Labels:      %s

                Description:
                %s

                Existing Comments:
                %s
                """.formatted(issueKey, summary, issueType, status, priority,
                assignee, reporter, labels.isBlank() ? "none" : labels,
                description.isBlank() ? "(no description)" : description,
                comments.isBlank() ? "(none)" : comments);
    }
}
