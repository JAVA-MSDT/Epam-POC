package com.javamsdt.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record AnalysisRequest(
        @JsonProperty("promptName") String promptName,
        @JsonProperty("context") Map<String, Object> context
) {
    public AnalysisRequest {
        if (promptName == null || promptName.isBlank()) {
            promptName = "analysis-prompt";
        }
        if (context == null) {
            context = Map.of();
        }
    }
}
