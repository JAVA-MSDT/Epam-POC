package com.javamsdt.agent.service;

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
import java.util.Map;

@Service
public class OptimizedJiraAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedJiraAnalysisService.class);

    private final ChatClient chatClient;
    private final JiraRetrievalTool jiraRetrievalTool;
    private final FileSystemTool fileSystemTool;
    private final OptimizedPromptPluginManager promptPluginManager;

    public OptimizedJiraAnalysisService(ChatClient.Builder chatClientBuilder,
                                        JiraRetrievalTool jiraRetrievalTool,
                                        FileSystemTool fileSystemTool,
                                        OptimizedPromptPluginManager promptPluginManager) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are an expert Jira ticket analyst with deep knowledge of software development processes.")
                .build();
        this.jiraRetrievalTool = jiraRetrievalTool;
        this.fileSystemTool = fileSystemTool;
        this.promptPluginManager = promptPluginManager;
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
            PromptPlugin promptPlugin = promptPluginManager.loadPlugin(promptName,
                    Map.of("ticketId", ticketId, "context", context));

            if (promptPlugin == null) {
                throw new IllegalArgumentException("Prompt plugin not found: " + promptName);
            }

            logger.debug("Using prompt plugin: {} from source: {}", promptName, promptPlugin.source());

            TicketAnalysis analysis = chatClient.prompt()
                    .user(promptPlugin.content())
                    .tools(
                            jiraRetrievalTool.asFunctionCallback(),
                            fileSystemTool.asFunctionCallback()
                    )
                    .call()
                    .entity(TicketAnalysis.class);

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
}
