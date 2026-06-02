package com.javamsdt.aidevworkflow.context;

/**
 * Shared mutable state that travels through all 8 workflow steps.
 * The orchestrator passes this single instance to every agent;
 * each agent reads its required inputs and writes its output back.
 * <p>
 * Field naming mirrors the step that produces each value, making
 * the data flow self-documenting.
 */
public class WorkflowContext {

    // ── Step 0: input ─────────────────────────────────────────────
    private String ticketText;

    // ── Step 1 output: Ticket Analysis ────────────────────────────
    private String ticketSummary;

    // ── Step 2 output: Project Setup ──────────────────────────────
    private String projectSetup;

    // ── Step 3 output: Deep Dive Analysis ─────────────────────────
    private String deepDive;

    // ── Step 4 output: Visual Analysis Report ─────────────────────
    private String visualReport;

    // ── Step 5 output: Review & Clarification ─────────────────────
    private String reviewNotes;

    // ── Step 6 output: Implementation ─────────────────────────────
    private String implementation;

    // ── Step 7 output: Quality Assurance ──────────────────────────
    private String qaReport;

    // ── Step 8 output: Deployment & Review ────────────────────────
    private String deploymentStatus;

    // ── Getters & Setters ──────────────────────────────────────────

    public String getTicketText() {
        return ticketText;
    }

    public void setTicketText(String ticketText) {
        this.ticketText = ticketText;
    }

    public String getTicketSummary() {
        return ticketSummary;
    }

    public void setTicketSummary(String ticketSummary) {
        this.ticketSummary = ticketSummary;
    }

    public String getProjectSetup() {
        return projectSetup;
    }

    public void setProjectSetup(String projectSetup) {
        this.projectSetup = projectSetup;
    }

    public String getDeepDive() {
        return deepDive;
    }

    public void setDeepDive(String deepDive) {
        this.deepDive = deepDive;
    }

    public String getVisualReport() {
        return visualReport;
    }

    public void setVisualReport(String visualReport) {
        this.visualReport = visualReport;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getQaReport() {
        return qaReport;
    }

    public void setQaReport(String qaReport) {
        this.qaReport = qaReport;
    }

    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    @Override
    public String toString() {
        return "WorkflowContext{" +
                "ticketSummary='" + truncate(ticketSummary) + '\'' +
                ", projectSetup='" + truncate(projectSetup) + '\'' +
                ", deepDive='" + truncate(deepDive) + '\'' +
                ", visualReport='" + truncate(visualReport) + '\'' +
                ", reviewNotes='" + truncate(reviewNotes) + '\'' +
                ", implementation='" + truncate(implementation) + '\'' +
                ", qaReport='" + truncate(qaReport) + '\'' +
                ", deploymentStatus='" + truncate(deploymentStatus) + '\'' +
                '}';
    }

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() > 60 ? value.substring(0, 60) + "..." : value;
    }
}
