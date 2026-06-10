package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.github.GitClient;
import com.javamsdt.aidevworkflow.github.GitHubClient;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.HtmlReportWriter;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 8 — Deployment & Review.
 * <p>
 * Reads:  ctx.implementation, ctx.qaReport, ctx.projectRootPath,
 * ctx.jiraTicketId, ctx.htmlReportPath, ctx.reportFolderPath
 * Writes: ctx.deploymentStatus, ctx.featureBranchName, ctx.committedFiles (execute)
 * ctx.prUrl, ctx.prComments (pushAndCreatePr)
 * <p>
 * Two-phase workflow:
 * Phase 1 — execute(): generates deployment plan, creates feature branch,
 * and makes one local git commit per logical group from the plan.
 * Does NOT push — branch stays local until human approval.
 * Phase 2 — pushAndCreatePr(): called by the orchestrator only after the user
 * confirms. Pushes the branch, opens a GitHub PR, fetches PR comments,
 * and appends them to the HTML report.
 */
public class DeploymentAgent {

    private static final String BASE_BRANCH = "main";

    // Parses "#### Commit N\nMessage: `<msg>`\nFiles:\n- path\n- path" blocks
    private static final Pattern COMMIT_GROUP_PATTERN = Pattern.compile(
            "####\\s+Commit\\s+\\d+[^\\n]*\\nMessage:\\s+`([^`]+)`\\nFiles:\\n((?:-\\s+[^\\n]+\\n?)+)",
            Pattern.MULTILINE
    );
    private static final Pattern FILE_LINE_PATTERN = Pattern.compile("-\\s+(.+)");

    private final LlmClient llmClient;
    private final GitClient gitClient;
    private final GitHubClient gitHubClient;

    /**
     * Full constructor — git and GitHub integration enabled.
     */
    public DeploymentAgent(LlmClient llmClient, GitClient gitClient, GitHubClient gitHubClient) {
        this.llmClient = llmClient;
        this.gitClient = gitClient;
        this.gitHubClient = gitHubClient;
    }

    /**
     * Convenience constructor — no git/GitHub; generates deployment plan text only.
     */
    public DeploymentAgent(LlmClient llmClient) {
        this(llmClient, null, null);
    }

    /**
     * Phase 1 — generates the deployment plan, creates a local feature branch,
     * and commits written files in logical groups. Does NOT push.
     * Call {@link #pushAndCreatePr(WorkflowContext)} after human approval.
     */
    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/deployment.md")
                .replace("{{implementation}}", ctx.getImplementation())
                .replace("{{qa_report}}", ctx.getQaReport());

        String deploymentStatus = llmClient.completePrompt(prompt);
        ctx.setDeploymentStatus(deploymentStatus);

        if (gitClient != null) {
            makeLocalCommits(ctx);
        } else {
            System.out.println("[DeploymentAgent] No git client — skipping branch creation and local commits.");
        }
    }

    /**
     * Phase 2 — pushes the feature branch and opens a GitHub PR.
     * Called by the orchestrator only after the user approves at Gate 3.
     *
     * @param ctx workflow context; must have featureBranchName set by execute()
     */
    public void pushAndCreatePr(WorkflowContext ctx) {
        if (gitClient == null || gitHubClient == null) {
            System.out.println("[DeploymentAgent] No git/GitHub clients — skipping push and PR creation.");
            return;
        }
        String branchName = ctx.getFeatureBranchName();
        if (branchName == null || branchName.isBlank()) {
            System.out.println("[DeploymentAgent] No feature branch recorded — nothing to push.");
            return;
        }
        try {
            System.out.println("[DeploymentAgent] Pushing branch: " + branchName);
            gitClient.push("origin", branchName);

            String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "feature";
            String prTitle = ticketId + ": " + extractFirstLine(ctx.getDeploymentStatus());
            String prBody = buildPrBody(ctx);
            System.out.println("[DeploymentAgent] Creating GitHub PR...");
            String prUrl = gitHubClient.createPullRequest(prTitle, prBody, branchName, BASE_BRANCH);
            ctx.setPrUrl(prUrl);
            System.out.println("[DeploymentAgent] PR created: " + prUrl);

            fetchAndAppendPrComments(ctx, prUrl);

        } catch (Exception e) {
            System.err.println("[DeploymentAgent] Push/PR failed: " + e.getMessage());
            ctx.setDeploymentStatus(ctx.getDeploymentStatus()
                    + "\n\n**Push/PR Error:** " + e.getMessage());
        }
    }

    private void makeLocalCommits(WorkflowContext ctx) {
        String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "feature";
        String branchName = "feature/" + ticketId.toLowerCase().replace(" ", "-");

        try {
            System.out.println("[DeploymentAgent] Creating branch: " + branchName);
            gitClient.createBranch(branchName);
            ctx.setFeatureBranchName(branchName);

            List<CommitGroup> groups = parseCommitGroups(ctx.getDeploymentStatus());
            if (groups.isEmpty()) {
                System.out.println("[DeploymentAgent] No commit groups parsed — falling back to single commitAll.");
                String fallbackMsg = "feat(" + ticketId.toLowerCase() + "): implement AI workflow changes";
                gitClient.commitAll(fallbackMsg);
                ctx.getCommittedFiles().addAll(ctx.getWrittenFiles());
            } else {
                for (CommitGroup group : groups) {
                    System.out.println("[DeploymentAgent] Committing group: " + group.message);
                    try {
                        gitClient.commitFiles(group.files, group.message);
                        ctx.getCommittedFiles().addAll(group.files);
                    } catch (Exception e) {
                        System.err.println("[DeploymentAgent] Commit failed for group '" + group.message + "': " + e.getMessage());
                    }
                }
            }
            System.out.println("[DeploymentAgent] Local commits done. " + ctx.getCommittedFiles().size() + " file(s) committed.");

        } catch (Exception e) {
            System.err.println("[DeploymentAgent] Branch/commit failed: " + e.getMessage());
            ctx.setDeploymentStatus(ctx.getDeploymentStatus()
                    + "\n\n**Git Error:** " + e.getMessage());
        }
    }

    private List<CommitGroup> parseCommitGroups(String deploymentStatus) {
        List<CommitGroup> groups = new ArrayList<>();
        Matcher groupMatcher = COMMIT_GROUP_PATTERN.matcher(deploymentStatus);
        while (groupMatcher.find()) {
            String message = groupMatcher.group(1).trim();
            String filesBlock = groupMatcher.group(2);
            List<String> files = new ArrayList<>();
            Matcher fileMatcher = FILE_LINE_PATTERN.matcher(filesBlock);
            while (fileMatcher.find()) {
                files.add(fileMatcher.group(1).trim());
            }
            if (!files.isEmpty()) {
                groups.add(new CommitGroup(message, files));
            }
        }
        return groups;
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

    private record CommitGroup(String message, List<String> files) {
    }
}
