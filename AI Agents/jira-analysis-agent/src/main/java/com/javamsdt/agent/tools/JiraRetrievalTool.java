package com.javamsdt.agent.tools;

import com.javamsdt.agent.config.JiraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class JiraRetrievalTool {

    private static final Logger logger = LoggerFactory.getLogger(JiraRetrievalTool.class);

    private final WebClient jiraWebClient;
    private final JiraProperties jiraProperties;

    public JiraRetrievalTool(WebClient jiraWebClient, JiraProperties jiraProperties) {
        this.jiraWebClient = jiraWebClient;
        this.jiraProperties = jiraProperties;
    }

    @Tool(name = "retrieve_jira_ticket",
          description = "Retrieve a Jira ticket by its ID or key and return its full details as JSON")
    public String retrieveJiraTicket(String ticketId) {
        if (!jiraProperties.isConfigured()) {
            logger.warn("Jira not configured — returning stub data for ticket: {}", ticketId);
            return buildStubTicket(ticketId);
        }

        try {
            logger.debug("Fetching Jira ticket: {}", ticketId);
            return jiraWebClient.get()
                    .uri("/rest/api/2/issue/{id}?expand=description,comment,labels,priority", ticketId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("Jira API error for ticket {}: {} {}", ticketId, e.getStatusCode(), e.getMessage());
            return "{\"error\": \"Jira API error: " + e.getStatusCode() + " - " + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("Failed to retrieve ticket {}: {}", ticketId, e.getMessage());
            return "{\"error\": \"Failed to retrieve ticket: " + e.getMessage() + "\"}";
        }
    }

    @Tool(name = "search_jira_tickets",
          description = "Search Jira tickets using a JQL query and return matching results as JSON array")
    public String searchJiraTickets(String jql) {
        if (!jiraProperties.isConfigured()) {
            logger.warn("Jira not configured — returning empty results for JQL: {}", jql);
            return "{\"issues\": [], \"total\": 0}";
        }

        try {
            logger.debug("Searching Jira with JQL: {}", jql);
            return jiraWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/2/search")
                            .queryParam("jql", jql)
                            .queryParam("maxResults", 20)
                            .queryParam("fields", "summary,status,priority,assignee,labels,description")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("Jira search error for JQL '{}': {} {}", jql, e.getStatusCode(), e.getMessage());
            return "{\"issues\": [], \"total\": 0, \"error\": \"" + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("Failed to search Jira with JQL '{}': {}", jql, e.getMessage());
            return "{\"issues\": [], \"total\": 0}";
        }
    }

    private String buildStubTicket(String ticketId) {
        return """
                {
                  "id": "%s",
                  "key": "%s",
                  "fields": {
                    "summary": "Stub ticket — configure jira.base-url, jira.username, jira.api-token to fetch real data",
                    "description": "This is placeholder data. The agent is running without Jira credentials.",
                    "status": { "name": "Open" },
                    "priority": { "name": "Medium" },
                    "labels": [],
                    "assignee": null,
                    "reporter": null,
                    "created": "2024-01-01T00:00:00.000+0000",
                    "updated": "2024-01-01T00:00:00.000+0000"
                  }
                }
                """.formatted(ticketId, ticketId);
    }

    public JiraRetrievalTool asFunctionCallback() {
        return this;
    }
}
