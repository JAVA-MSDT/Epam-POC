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

    // ── External inputs (set before workflow starts) ───────────────
    /** Jira ticket ID (e.g. "PROJ-123"). Used by TicketAnalysisAgent to fetch from Jira. */
    private String jiraTicketId;

    /** The root path of the target project being analyzed (used by DeepDiveAgent for code scanning). */
    private String projectRootPath;

    // ── Step 0: ticket input ───────────────────────────────────────
    private String ticketText;

    // ── Step 1 output: Ticket Analysis ────────────────────────────
    private String ticketSummary;

    // ── Step 2 output: Project Setup ──────────────────────────────
    private String projectSetup;

    /** Absolute path to the report folder created by ProjectSetupAgent. */
    private String reportFolderPath;

    // ── Step 3 output: Deep Dive Analysis ─────────────────────────
    private String deepDive;

    // ── Step 4 output: Visual Analysis Report ─────────────────────
    private String visualReport;

    /** Absolute path to the HTML report file written by VisualReportAgent. */
    private String htmlReportPath;

    // ── Step 5 output: Review & Clarification ─────────────────────
    private String reviewNotes;

    // ── Step 6 output: Implementation ─────────────────────────────
    private String implementation;

    // ── Step 7 output: Quality Assurance ──────────────────────────
    private String qaReport;

    // ── Step 8 output: Deployment & Review ────────────────────────
    private String deploymentStatus;

    /** URL of the GitHub PR created by DeploymentAgent. */
    private String prUrl;

    /** Raw PR comments fetched from GitHub, used to update the HTML report. */
    private String prComments;

    // ── Getters & Setters ──────────────────────────────────────────

    public String getJiraTicketId() { return jiraTicketId; }
    public void setJiraTicketId(String jiraTicketId) { this.jiraTicketId = jiraTicketId; }

    public String getProjectRootPath() { return projectRootPath; }
    public void setProjectRootPath(String projectRootPath) { this.projectRootPath = projectRootPath; }

    public String getReportFolderPath() { return reportFolderPath; }
    public void setReportFolderPath(String reportFolderPath) { this.reportFolderPath = reportFolderPath; }

    public String getHtmlReportPath() { return htmlReportPath; }
    public void setHtmlReportPath(String htmlReportPath) { this.htmlReportPath = htmlReportPath; }

    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }

    public String getPrComments() { return prComments; }
    public void setPrComments(String prComments) { this.prComments = prComments; }

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
                "jiraTicketId='" + jiraTicketId + '\'' +
                ", projectRootPath='" + projectRootPath + '\'' +
                ", ticketSummary='" + truncate(ticketSummary) + '\'' +
                ", projectSetup='" + truncate(projectSetup) + '\'' +
                ", reportFolderPath='" + reportFolderPath + '\'' +
                ", deepDive='" + truncate(deepDive) + '\'' +
                ", visualReport='" + truncate(visualReport) + '\'' +
                ", htmlReportPath='" + htmlReportPath + '\'' +
                ", reviewNotes='" + truncate(reviewNotes) + '\'' +
                ", implementation='" + truncate(implementation) + '\'' +
                ", qaReport='" + truncate(qaReport) + '\'' +
                ", deploymentStatus='" + truncate(deploymentStatus) + '\'' +
                ", prUrl='" + prUrl + '\'' +
                '}';
    }

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() > 60 ? value.substring(0, 60) + "..." : value;
    }
}
