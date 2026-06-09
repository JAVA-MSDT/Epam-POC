package com.javamsdt.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class JiraRetrievalTool {

    @Tool(description = "Retrieve a Jira ticket by its ID and return its details as JSON")
    public String getTicket(String ticketId) {
        // TODO: replace with real Jira REST API call
        return "{\"id\": \"" + ticketId + "\", \"summary\": \"Placeholder ticket\", \"status\": \"Open\"}";
    }

    @Tool(description = "Search Jira tickets using a JQL query and return matching results as JSON array")
    public String searchTickets(String jql) {
        // TODO: replace with real Jira REST API call
        return "[]";
    }

    /** Returns this instance as a Spring AI tool object (objects with @Tool methods are accepted directly). */
    public JiraRetrievalTool asFunctionCallback() {
        return this;
    }
}
