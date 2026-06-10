package com.javamsdt.agent.tools;

import com.javamsdt.agent.config.JiraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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

        logger.info("[STEP 1] Fetching Jira ticket: {} from base URL: {}", ticketId, jiraProperties.getBaseUrl());

        try {
            String responseBody = jiraWebClient.get()
                    .uri("/rest/api/2/issue/{id}", ticketId)
                    .exchangeToMono(this::readResponse)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                logger.error("[STEP 1] Jira returned null/empty body for ticket: {}", ticketId);
                return "{\"error\": \"Jira returned empty response for ticket: " + ticketId + "\"}";
            }

            logger.info("[STEP 1] Successfully fetched ticket: {} ({} chars)", ticketId, responseBody.length());
            logger.info("[STEP 1] Raw Jira response:\n{}", responseBody);
            return responseBody;

        } catch (WebClientResponseException e) {
            logger.error("[STEP 1] Jira HTTP error for ticket {}: {} — body: {}",
                    ticketId, e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"error\": \"Jira HTTP " + e.getStatusCode() + " for ticket " + ticketId + ": " + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("[STEP 1] Failed to retrieve ticket {}: {}", ticketId, e.getMessage(), e);
            return "{\"error\": \"Failed to retrieve ticket " + ticketId + ": " + e.getMessage() + "\"}";
        }
    }

    @Tool(name = "search_jira_tickets",
          description = "Search Jira tickets using a JQL query and return matching results as JSON array")
    public String searchJiraTickets(String jql) {
        if (!jiraProperties.isConfigured()) {
            logger.warn("Jira not configured — returning empty results for JQL: {}", jql);
            return "{\"issues\": [], \"total\": 0}";
        }

        logger.info("[STEP 2] Searching Jira with JQL: {}", jql);

        try {
            String responseBody = jiraWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/2/search")
                            .queryParam("jql", jql)
                            .queryParam("maxResults", 20)
                            .queryParam("fields", "summary,status,priority,assignee,labels,description")
                            .build())
                    .exchangeToMono(this::readResponse)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                logger.warn("[STEP 2] Jira search returned empty body for JQL: {}", jql);
                return "{\"issues\": [], \"total\": 0}";
            }

            logger.info("[STEP 2] Search completed for JQL: {} ({} chars)", jql, responseBody.length());
            logger.debug("[STEP 2] Search response:\n{}", responseBody);
            return responseBody;

        } catch (WebClientResponseException e) {
            logger.error("[STEP 2] Jira search HTTP error for JQL '{}': {} — body: {}",
                    jql, e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"issues\": [], \"total\": 0, \"error\": \"HTTP " + e.getStatusCode() + "\"}";
        } catch (Exception e) {
            logger.error("[STEP 2] Failed to search Jira with JQL '{}': {}", jql, e.getMessage(), e);
            return "{\"issues\": [], \"total\": 0}";
        }
    }

    private Mono<String> readResponse(ClientResponse response) {
        logger.info("Jira API response status: {}", response.statusCode());
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class);
        }
        return response.bodyToMono(String.class)
                .flatMap(body -> Mono.error(new WebClientResponseException(
                        response.statusCode().value(),
                        response.statusCode().toString(),
                        response.headers().asHttpHeaders(),
                        body.getBytes(),
                        null)));
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
