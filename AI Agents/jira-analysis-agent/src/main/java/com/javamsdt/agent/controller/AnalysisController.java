package com.javamsdt.agent.controller;

import com.javamsdt.agent.model.AnalysisRequest;
import com.javamsdt.agent.model.TicketAnalysis;
import com.javamsdt.agent.service.OptimizedJiraAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    private final OptimizedJiraAnalysisService analysisService;

    public AnalysisController(OptimizedJiraAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketAnalysis> analyzeTicket(@PathVariable String ticketId) {
        logger.info("Analysis request for ticket: {}", ticketId);
        TicketAnalysis analysis = analysisService.analyzeTicket(ticketId);
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/tickets/{ticketId}/security")
    public ResponseEntity<TicketAnalysis> analyzeTicketSecurity(@PathVariable String ticketId) {
        logger.info("Security analysis request for ticket: {}", ticketId);
        TicketAnalysis analysis = analysisService.analyzeTicket(ticketId, "security-analysis-prompt", Map.of());
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/tickets/{ticketId}/custom")
    public ResponseEntity<TicketAnalysis> analyzeTicketCustom(
            @PathVariable String ticketId,
            @RequestBody(required = false) AnalysisRequest request) {
        if (request == null) {
            request = new AnalysisRequest(null, null);
        }
        logger.info("Custom analysis request for ticket: {} using prompt: {}", ticketId, request.promptName());
        TicketAnalysis analysis = analysisService.analyzeTicket(ticketId, request.promptName(), request.context());
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/tickets/{ticketId}/cached")
    public ResponseEntity<TicketAnalysis> getCachedAnalysis(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "analysis-prompt") String promptName) {
        logger.info("Cached analysis request for ticket: {} with prompt: {}", ticketId, promptName);
        TicketAnalysis analysis = analysisService.getCachedAnalysis(ticketId, promptName);
        return ResponseEntity.ok(analysis);
    }
}
