package com.javamsdt.agent.controller;

import com.javamsdt.agent.service.OptimizedJiraAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final OptimizedJiraAnalysisService analysisService;

    public MonitoringController(OptimizedJiraAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean isHealthy = analysisService.isHealthy();

        return ResponseEntity.ok(Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", System.currentTimeMillis(),
                "service", "jira-analysis-agent"
        ));
    }

    @GetMapping("/plugin-stats")
    public ResponseEntity<Map<String, Object>> getPluginStatistics() {
        return ResponseEntity.ok(analysisService.getPluginStatistics());
    }
}
