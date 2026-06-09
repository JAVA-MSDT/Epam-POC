package com.javamsdt.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record TicketAnalysis(
        @JsonProperty("ticket_id") @NotBlank String ticketId,
        @JsonProperty("summary") @NotBlank String summary,
        @JsonProperty("requirements_analysis") @NotNull RequirementsAnalysis requirementsAnalysis,
        @JsonProperty("technical_analysis") @NotNull TechnicalAnalysis technicalAnalysis,
        @JsonProperty("risk_assessment") @NotNull RiskAssessment riskAssessment,
        @JsonProperty("effort_estimation") @NotNull EffortEstimation effortEstimation,
        @JsonProperty("implementation_strategy") @NotNull ImplementationStrategy implementationStrategy,
        @JsonProperty("analysis_metadata") @NotNull AnalysisMetadata metadata
) {

    public record RequirementsAnalysis(
            @JsonProperty("functional_requirements") List<String> functionalRequirements,
            @JsonProperty("non_functional_requirements") List<String> nonFunctionalRequirements,
            @JsonProperty("acceptance_criteria") List<String> acceptanceCriteria,
            @JsonProperty("dependencies") List<String> dependencies,
            @JsonProperty("assumptions") List<String> assumptions
    ) {}

    public record TechnicalAnalysis(
            @JsonProperty("complexity_score") int complexityScore,
            @JsonProperty("technical_challenges") List<String> technicalChallenges,
            @JsonProperty("recommended_approach") String recommendedApproach,
            @JsonProperty("architecture_considerations") List<String> architectureConsiderations,
            @JsonProperty("technology_stack") List<String> technologyStack,
            @JsonProperty("performance_considerations") List<String> performanceConsiderations
    ) {}

    public record RiskAssessment(
            @JsonProperty("identified_risks") List<Risk> identifiedRisks,
            @JsonProperty("overall_risk_level") String overallRiskLevel,
            @JsonProperty("risk_score") int riskScore
    ) {
        public record Risk(
                @JsonProperty("description") String description,
                @JsonProperty("category") String category,
                @JsonProperty("impact") String impact,
                @JsonProperty("probability") String probability,
                @JsonProperty("mitigation_strategy") String mitigationStrategy,
                @JsonProperty("contingency_plan") String contingencyPlan
        ) {}
    }

    public record EffortEstimation(
            @JsonProperty("development_days") int developmentDays,
            @JsonProperty("testing_days") int testingDays,
            @JsonProperty("documentation_days") int documentationDays,
            @JsonProperty("review_days") int reviewDays,
            @JsonProperty("total_days") int totalDays,
            @JsonProperty("confidence_level") String confidenceLevel,
            @JsonProperty("estimation_method") String estimationMethod,
            @JsonProperty("team_size_assumption") int teamSizeAssumption
    ) {}

    public record ImplementationStrategy(
            @JsonProperty("phases") List<Phase> phases,
            @JsonProperty("key_milestones") List<Milestone> keyMilestones,
            @JsonProperty("success_criteria") List<String> successCriteria,
            @JsonProperty("rollback_strategy") String rollbackStrategy
    ) {
        public record Phase(
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("estimated_days") int estimatedDays,
                @JsonProperty("deliverables") List<String> deliverables,
                @JsonProperty("dependencies") List<String> dependencies,
                @JsonProperty("risks") List<String> risks
        ) {}

        public record Milestone(
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("target_date") String targetDate,
                @JsonProperty("success_criteria") List<String> successCriteria
        ) {}
    }

    public record AnalysisMetadata(
            @JsonProperty("analysis_timestamp") LocalDateTime analysisTimestamp,
            @JsonProperty("model_used") String modelUsed,
            @JsonProperty("analysis_version") String analysisVersion,
            @JsonProperty("processing_time_ms") long processingTimeMs,
            @JsonProperty("prompt_name") String promptName,
            @JsonProperty("prompt_source") String promptSource,
            @JsonProperty("prompt_last_modified") LocalDateTime promptLastModified
    ) {}
}
