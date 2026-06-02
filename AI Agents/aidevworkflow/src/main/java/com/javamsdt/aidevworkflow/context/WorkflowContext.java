package com.javamsdt.aidevworkflow.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Cached codebase snapshot produced by DeepDiveAgent on its first run.
     * Stored here so QualityAssuranceAgent and ImplementationAgent can reuse it
     * without triggering a second filesystem scan. Set to null to force a re-scan.
     */
    private String codebaseSnapshot;

    // ── Progress tracking (populated during Step 6 implementation) ─
    /** Absolute paths of files successfully written to disk by ImplementationAgent. */
    private List<String> writtenFiles = new ArrayList<>();

    /** Relative paths extracted from LLM response not yet written (decrements as files are written). */
    private List<String> pendingFiles = new ArrayList<>();

    /**
     * Per-file QA status. Key = absolute file path, value = one of:
     * "PENDING", "REVIEWED", "PASS".
     */
    private Map<String, String> fileQaStatus = new LinkedHashMap<>();

    /** Absolute paths of files included in a local git commit by DeploymentAgent. */
    private List<String> committedFiles = new ArrayList<>();

    /** Monotonically increasing counter incremented each time ImplementationAgent writes a file. */
    private int implementationStep = 0;

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

    public String getCodebaseSnapshot() {
        return codebaseSnapshot;
    }

    public void setCodebaseSnapshot(String codebaseSnapshot) {
        this.codebaseSnapshot = codebaseSnapshot;
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

    public List<String> getWrittenFiles() { return writtenFiles; }
    public void setWrittenFiles(List<String> writtenFiles) { this.writtenFiles = writtenFiles; }

    public List<String> getPendingFiles() { return pendingFiles; }
    public void setPendingFiles(List<String> pendingFiles) { this.pendingFiles = pendingFiles; }

    public Map<String, String> getFileQaStatus() { return fileQaStatus; }
    public void setFileQaStatus(Map<String, String> fileQaStatus) { this.fileQaStatus = fileQaStatus; }

    public List<String> getCommittedFiles() { return committedFiles; }
    public void setCommittedFiles(List<String> committedFiles) { this.committedFiles = committedFiles; }

    public int getImplementationStep() { return implementationStep; }
    public void setImplementationStep(int implementationStep) { this.implementationStep = implementationStep; }

    @Override
    public String toString() {
        return "WorkflowContext{" +
                "jiraTicketId='" + jiraTicketId + '\'' +
                ", projectRootPath='" + projectRootPath + '\'' +
                ", ticketSummary='" + truncate(ticketSummary) + '\'' +
                ", projectSetup='" + truncate(projectSetup) + '\'' +
                ", reportFolderPath='" + reportFolderPath + '\'' +
                ", deepDive='" + truncate(deepDive) + '\'' +
                ", codebaseSnapshot='" + (codebaseSnapshot != null ? "[cached, " + codebaseSnapshot.length() + " chars]" : "null") + '\'' +
                ", visualReport='" + truncate(visualReport) + '\'' +
                ", htmlReportPath='" + htmlReportPath + '\'' +
                ", reviewNotes='" + truncate(reviewNotes) + '\'' +
                ", implementation='" + truncate(implementation) + '\'' +
                ", qaReport='" + truncate(qaReport) + '\'' +
                ", deploymentStatus='" + truncate(deploymentStatus) + '\'' +
                ", prUrl='" + prUrl + '\'' +
                ", writtenFiles=" + writtenFiles.size() +
                ", pendingFiles=" + pendingFiles.size() +
                ", committedFiles=" + committedFiles.size() +
                ", implementationStep=" + implementationStep +
                ", fileQaStatus=" + fileQaStatus.size() + " entries" +
                '}';
    }

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() > 60 ? value.substring(0, 60) + "..." : value;
    }
}
