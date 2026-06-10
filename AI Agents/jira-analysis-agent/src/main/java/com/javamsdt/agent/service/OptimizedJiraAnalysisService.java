package com.javamsdt.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javamsdt.agent.config.plugin.OptimizedPromptPluginManager;
import com.javamsdt.agent.config.plugin.PromptPlugin;
import com.javamsdt.agent.model.TicketAnalysis;
import com.javamsdt.agent.tools.FileSystemTool;
import com.javamsdt.agent.tools.JiraRetrievalTool;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OptimizedJiraAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedJiraAnalysisService.class);

    private static final String JSON_ONLY_REMINDER =
            "\n\nIMPORTANT: Output ONLY valid JSON. Start with { and end with }. No text before or after.";

    private final ChatClient chatClient;
    private final JiraRetrievalTool jiraRetrievalTool;
    private final FileSystemTool fileSystemTool;
    private final OptimizedPromptPluginManager promptPluginManager;
    private final ObjectMapper objectMapper;

    public OptimizedJiraAnalysisService(ChatClient.Builder chatClientBuilder,
                                        JiraRetrievalTool jiraRetrievalTool,
                                        FileSystemTool fileSystemTool,
                                        OptimizedPromptPluginManager promptPluginManager,
                                        ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.jiraRetrievalTool = jiraRetrievalTool;
        this.fileSystemTool = fileSystemTool;
        this.promptPluginManager = promptPluginManager;
        this.objectMapper = objectMapper;
    }

    @Observed(name = "jira.analysis.complete", contextualName = "jira-analysis-complete")
    @Timed(value = "jira.analysis.duration", description = "Time taken to analyze a ticket")
    public TicketAnalysis analyzeTicket(String ticketId) {
        return analyzeTicket(ticketId, "analysis-prompt", Map.of());
    }

    @Observed(name = "jira.analysis.custom", contextualName = "jira-analysis-custom")
    @Timed(value = "jira.analysis.custom.duration", description = "Time taken for custom analysis")
    public TicketAnalysis analyzeTicket(String ticketId, String promptName, Map<String, Object> context) {
        logger.info("Starting analysis for ticket: {} using prompt: {}", ticketId, promptName);
        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: collect all Jira data directly in Java (no LLM involved)
            JiraData jiraData = collectJiraData(ticketId);

            // Phase 2: load prompt template and substitute all variables
            PromptPlugin promptPlugin = promptPluginManager.loadPlugin(promptName,
                    Map.of("ticketId", ticketId, "context", context));

            if (promptPlugin == null) {
                throw new IllegalArgumentException("Prompt plugin not found: " + promptName);
            }

            logger.debug("Using prompt plugin: {} from source: {}", promptName, promptPlugin.source());

            // Extract only the fields the LLM needs — avoids flooding context with raw Jira metadata
            String ticketData = extractEssentialTicketData(jiraData.ticketJson());
            String linkedData = extractEssentialLinkedData(jiraData.linkedIssuesJson());

            logger.info("[LLM] Compact ticket data ({} chars):\n{}", ticketData.length(), ticketData);
            logger.info("[LLM] Compact linked issues data ({} chars)", linkedData.length());

            String promptContent = promptPlugin.content()
                    .replace("{{ticketData}}", ticketData)
                    .replace("{{linkedIssuesData}}", linkedData)
                    + JSON_ONLY_REMINDER;

            logger.info("[LLM] Sending prompt to LLM ({} chars total)", promptContent.length());

            // Phase 3: LLM call — analysis only, no tool calls
            TicketAnalysis analysis = callLlmForAnalysis(promptContent, ticketId);

            long processingTime = System.currentTimeMillis() - startTime;
            TicketAnalysis enhancedAnalysis = enhanceWithMetadata(analysis, processingTime, promptPlugin);

            logger.info("Analysis completed for ticket: {} in {}ms using prompt: {}",
                    ticketId, processingTime, promptName);

            return enhancedAnalysis;

        } catch (Exception e) {
            logger.error("Analysis failed for ticket {}: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze ticket: " + ticketId, e);
        }
    }

    private JiraData collectJiraData(String ticketId) {
        // --- Step 1: fetch the ticket ---
        long t0 = System.currentTimeMillis();
        String ticketJson = jiraRetrievalTool.retrieveJiraTicket(ticketId);
        logger.info("[COLLECT] Ticket fetch completed in {}ms", System.currentTimeMillis() - t0);

        if (ticketJson == null || ticketJson.isBlank()) {
            logger.error("[COLLECT] Ticket JSON is null/empty for {}. Check Jira URL and credentials.", ticketId);
            ticketJson = "{\"error\": \"no data returned for ticket " + ticketId + "\"}";
        } else {
            logger.info("[COLLECT] Ticket JSON length: {} chars", ticketJson.length());
        }

        // --- Step 2: extract linked issues and search ---
        List<String> linkedKeys = extractLinkedIssueKeys(ticketJson);
        logger.info("[COLLECT] Linked issue keys parsed from ticket: {}", linkedKeys);

        String linkedIssuesJson = "{\"issues\": [], \"total\": 0}";
        if (!linkedKeys.isEmpty()) {
            String jql = "key in (" + String.join(", ", linkedKeys) + ")";
            long t1 = System.currentTimeMillis();
            linkedIssuesJson = jiraRetrievalTool.searchJiraTickets(jql);
            logger.info("[COLLECT] Linked issues search completed in {}ms", System.currentTimeMillis() - t1);
        } else {
            logger.info("[COLLECT] No linked issues — skipping search");
        }

        // --- Step 3: create output folder ---
        String folderPath = fileSystemTool.createTicketFolder(ticketId);
        logger.info("[COLLECT] Output folder: {}", folderPath);

        return new JiraData(ticketJson, linkedIssuesJson);
    }

    private String extractEssentialTicketData(String rawTicketJson) {
        if (rawTicketJson == null || rawTicketJson.isBlank()) {
            return "{}";
        }
        try {
            JsonNode root = objectMapper.readTree(rawTicketJson);
            JsonNode fields = root.path("fields");

            ObjectNode compact = objectMapper.createObjectNode();
            compact.put("key", root.path("key").asText(""));
            compact.put("summary", fields.path("summary").asText(""));
            compact.put("description", fields.path("description").asText(""));
            compact.put("issuetype", fields.path("issuetype").path("name").asText(""));
            compact.put("status", fields.path("status").path("name").asText(""));
            compact.put("priority", fields.path("priority").path("name").asText(""));
            compact.put("created", fields.path("created").asText(""));
            compact.put("updated", fields.path("updated").asText(""));

            JsonNode assignee = fields.path("assignee");
            if (!assignee.isMissingNode() && !assignee.isNull()) {
                compact.put("assignee", assignee.path("displayName").asText(""));
            }

            JsonNode reporter = fields.path("reporter");
            if (!reporter.isMissingNode() && !reporter.isNull()) {
                compact.put("reporter", reporter.path("displayName").asText(""));
            }

            JsonNode labels = fields.path("labels");
            if (labels.isArray() && !labels.isEmpty()) {
                compact.set("labels", labels);
            }

            String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compact);
            logger.info("[EXTRACT] Reduced ticket from {} chars to {} chars", rawTicketJson.length(), result.length());
            return result;

        } catch (Exception e) {
            logger.warn("[EXTRACT] Could not extract essential fields, using raw: {}", e.getMessage());
            return rawTicketJson;
        }
    }

    private String extractEssentialLinkedData(String rawLinkedJson) {
        if (rawLinkedJson == null || rawLinkedJson.isBlank()) {
            return "{\"issues\": []}";
        }
        try {
            JsonNode root = objectMapper.readTree(rawLinkedJson);
            JsonNode issues = root.path("issues");
            if (!issues.isArray() || issues.isEmpty()) {
                return "{\"issues\": []}";
            }
            // Keep only key + summary + status for each linked issue
            var compactIssues = objectMapper.createArrayNode();
            for (JsonNode issue : issues) {
                ObjectNode compact = objectMapper.createObjectNode();
                compact.put("key", issue.path("key").asText(""));
                compact.put("summary", issue.path("fields").path("summary").asText(""));
                compact.put("status", issue.path("fields").path("status").path("name").asText(""));
                compactIssues.add(compact);
            }
            ObjectNode result = objectMapper.createObjectNode();
            result.set("issues", compactIssues);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.warn("[EXTRACT] Could not compact linked issues: {}", e.getMessage());
            return rawLinkedJson;
        }
    }

    private List<String> extractLinkedIssueKeys(String ticketJson) {
        List<String> keys = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(ticketJson);
            JsonNode issueLinks = root.path("fields").path("issuelinks");
            if (issueLinks.isArray()) {
                for (JsonNode link : issueLinks) {
                    JsonNode inward = link.path("inwardIssue").path("key");
                    JsonNode outward = link.path("outwardIssue").path("key");
                    if (!inward.isMissingNode()) keys.add(inward.asText());
                    if (!outward.isMissingNode()) keys.add(outward.asText());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse linked issues from ticket JSON: {}", e.getMessage());
        }
        return keys;
    }

    private TicketAnalysis callLlmForAnalysis(String promptContent, String ticketId) {
        long t = System.currentTimeMillis();
        String raw = chatClient.prompt()
                .user(promptContent)
                .call()
                .content();
        logger.info("[LLM] Response received in {}ms ({} chars)", System.currentTimeMillis() - t,
                raw != null ? raw.length() : 0);
        logger.info("[LLM] Raw response:\n{}", raw);
        return parseJsonResponse(raw, ticketId);
    }

    private TicketAnalysis parseJsonResponse(String rawResponse, String ticketId) {
        String json = rawResponse == null ? "" : rawResponse.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(json, TicketAnalysis.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse LLM JSON for ticket {}: {}", ticketId, e.getMessage());
            logger.error("Raw response was: {}", rawResponse);
            throw new RuntimeException("LLM did not return valid JSON for ticket: " + ticketId, e);
        }
    }

    @Cacheable(value = "analysisResults", key = "#ticketId + '_' + #promptName", unless = "#result == null")
    public TicketAnalysis getCachedAnalysis(String ticketId, String promptName) {
        return analyzeTicket(ticketId, promptName, Map.of());
    }

    private TicketAnalysis enhanceWithMetadata(TicketAnalysis analysis,
                                               long processingTime,
                                               PromptPlugin promptPlugin) {
        var enhancedMetadata = new TicketAnalysis.AnalysisMetadata(
                LocalDateTime.now(),
                "llama3.1:8b",
                "1.0.0",
                processingTime,
                promptPlugin.name(),
                promptPlugin.source().toString(),
                promptPlugin.lastModified()
        );

        return new TicketAnalysis(
                analysis.ticketId(),
                analysis.summary(),
                analysis.requirementsAnalysis(),
                analysis.technicalAnalysis(),
                analysis.riskAssessment(),
                analysis.effortEstimation(),
                analysis.implementationStrategy(),
                enhancedMetadata
        );
    }

    public boolean isHealthy() {
        try {
            PromptPlugin testPlugin = promptPluginManager.loadPlugin("analysis-prompt");
            return testPlugin != null;
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    public Map<String, Object> getPluginStatistics() {
        return promptPluginManager.getCacheStatistics();
    }

    private record JiraData(String ticketJson, String linkedIssuesJson) {}
}
