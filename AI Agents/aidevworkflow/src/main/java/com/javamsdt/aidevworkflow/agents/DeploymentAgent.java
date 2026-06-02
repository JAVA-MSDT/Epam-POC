package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.github.GitClient;
import com.javamsdt.aidevworkflow.github.GitHubClient;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.HtmlReportWriter;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 8 — Deployment & Review.
 *
 * Reads:  ctx.implementation, ctx.qaReport, ctx.projectRootPath,
 *         ctx.jiraTicketId, ctx.htmlReportPath, ctx.reportFolderPath
 * Writes: ctx.deploymentStatus, ctx.prUrl, ctx.prComments
 *
 * Workflow:
 *   1. Generates a deployment plan via LLM.
 *   2. Creates a git branch, commits all changes, and pushes to origin.
 *   3. Opens a GitHub PR and stores the URL in ctx.prUrl.
 *   4. Fetches PR review comments (if any) and stores them in ctx.prComments.
 *   5. Updates the HTML report with a "PR Comments" section.
 */
public class DeploymentAgent {

    private static final String BASE_BRANCH = "main";

    private final LlmClient llmClient;
    private final GitClient gitClient;
    private final GitHubClient gitHubClient;

    /** Full constructor — git and GitHub integration enabled. */
    public DeploymentAgent(LlmClient llmClient, GitClient gitClient, GitHubClient gitHubClient) {
        this.llmClient = llmClient;
        this.gitClient = gitClient;
        this.gitHubClient = gitHubClient;
    }

    /** Convenience constructor — no git/GitHub; generates deployment plan text only. */
    public DeploymentAgent(LlmClient llmClient) {
        this(llmClient, null, null);
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/deployment.md")
                .replace("{{implementation}}", ctx.getImplementation())
                .replace("{{qa_report}}", ctx.getQaReport());

        String deploymentStatus = llmClient.completePrompt(prompt);
        ctx.setDeploymentStatus(deploymentStatus);

        if (gitClient != null && gitHubClient != null) {
            runGitOps(ctx);
        } else {
            System.out.println("[DeploymentAgent] No git/GitHub clients — skipping commit, push, and PR creation.");
        }
    }

    private void runGitOps(WorkflowContext ctx) {
        String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "feature";
        String branchName = "feature/" + ticketId.toLowerCase().replace(" ", "-");

        try {
            System.out.println("[DeploymentAgent] Creating branch: " + branchName);
            gitClient.createBranch(branchName);

            String commitMsg = ticketId + ": implement changes from AI workflow";
            System.out.println("[DeploymentAgent] Committing: " + commitMsg);
            gitClient.commitAll(commitMsg);

            System.out.println("[DeploymentAgent] Pushing to origin/" + branchName);
            gitClient.push("origin", branchName);

            String prTitle = ticketId + ": " + extractFirstLine(ctx.getDeploymentStatus());
            String prBody = buildPrBody(ctx);
            System.out.println("[DeploymentAgent] Creating GitHub PR...");
            String prUrl = gitHubClient.createPullRequest(prTitle, prBody, branchName, BASE_BRANCH);
            ctx.setPrUrl(prUrl);
            System.out.println("[DeploymentAgent] PR created: " + prUrl);

            fetchAndAppendPrComments(ctx, prUrl);

        } catch (Exception e) {
            System.err.println("[DeploymentAgent] Git/GitHub operation failed: " + e.getMessage());
            ctx.setDeploymentStatus(ctx.getDeploymentStatus()
                    + "\n\n**Git/GitHub Error:** " + e.getMessage());
        }
    }

    private void fetchAndAppendPrComments(WorkflowContext ctx, String prUrl) {
        try {
            int prNumber = GitHubClient.prNumberFromUrl(prUrl);
            String comments = gitHubClient.fetchPrComments(prNumber);
            ctx.setPrComments(comments);
            System.out.println("[DeploymentAgent] Fetched PR comments.");

            if (!comments.isBlank() && !"(no review comments yet)".equals(comments)) {
                appendPrCommentsToHtmlReport(ctx, comments);
            }
        } catch (Exception e) {
            System.err.println("[DeploymentAgent] Could not fetch PR comments: " + e.getMessage());
        }
    }

    private void appendPrCommentsToHtmlReport(WorkflowContext ctx, String comments) {
        String reportPath = ctx.getHtmlReportPath();
        String reportFolder = ctx.getReportFolderPath();
        if (reportPath == null && reportFolder == null) return;

        String commentsSection = """
                <div class="section">
                  <h2>PR Review Comments</h2>
                  <p>The following comments were left on the pull request and require attention:</p>
                  <pre>%s</pre>
                </div>
                """.formatted(escapeHtml(comments));

        String targetPath = reportPath != null ? reportPath
                : reportFolder + "/analysis_report.html";

        try {
            String existing = com.javamsdt.aidevworkflow.util.FileSystemUtil.readFile(targetPath);
            String updated = existing.replace("</body>", commentsSection + "\n</body>");
            com.javamsdt.aidevworkflow.util.FileSystemUtil.writeFile(targetPath, updated);
            System.out.println("[DeploymentAgent] PR comments appended to HTML report: " + targetPath);
        } catch (Exception e) {
            System.err.println("[DeploymentAgent] Could not update HTML report with PR comments: " + e.getMessage());
        }
    }

    private String buildPrBody(WorkflowContext ctx) {
        String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "N/A";
        String reportPath = ctx.getHtmlReportPath() != null ? ctx.getHtmlReportPath() : "N/A";
        return """
                ## Summary

                This PR was generated by the AI Dev Workflow agent pipeline.

                **Ticket:** %s
                **Analysis Report:** `%s`

                ## QA Status

                %s

                ## Deployment Plan

                %s

                ---
                *Generated by aidevworkflow*
                """.formatted(ticketId, reportPath,
                truncate(ctx.getQaReport(), 1000),
                truncate(ctx.getDeploymentStatus(), 1000));
    }

    private String extractFirstLine(String text) {
        if (text == null || text.isBlank()) return "implementation";
        return text.lines().findFirst().orElse("implementation").replaceAll("[#*`]", "").trim();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
