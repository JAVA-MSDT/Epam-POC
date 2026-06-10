package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.FileSystemUtil;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 2 — Project Setup.
 * <p>
 * Reads:  ctx.ticketSummary, ctx.jiraTicketId, ctx.projectRootPath
 * Writes: ctx.projectSetup, ctx.reportFolderPath
 * <p>
 * In addition to generating the setup plan, this agent creates a local report
 * folder on disk (under projectRootPath/reports/<ticketId>) and stores its path
 * in ctx.reportFolderPath for use by later agents (VisualReportAgent etc.).
 */
public class ProjectSetupAgent {

    private static final String DEFAULT_REPORTS_BASE = "reports";

    private final LlmClient llmClient;

    public ProjectSetupAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String reportFolder = createReportFolder(ctx);
        ctx.setReportFolderPath(reportFolder);
        System.out.println("[ProjectSetupAgent] Report folder created: " + reportFolder);

        String prompt = MarkdownLoader.load("agents/project_setup.md")
                .replace("{{ticket_summary}}", ctx.getTicketSummary())
                .replace("{{report_folder}}", reportFolder);

        ctx.setProjectSetup(llmClient.completePrompt(prompt));
    }

    private String createReportFolder(WorkflowContext ctx) {
        String ticketId = ctx.getJiraTicketId();
        if (ticketId == null || ticketId.isBlank()) {
            ticketId = "ticket-" + System.currentTimeMillis();
        }

        String baseDir;
        if (ctx.getProjectRootPath() != null && !ctx.getProjectRootPath().isBlank()) {
            baseDir = ctx.getProjectRootPath() + "/" + DEFAULT_REPORTS_BASE;
        } else {
            baseDir = System.getProperty("user.dir") + "/" + DEFAULT_REPORTS_BASE;
        }

        return FileSystemUtil.createReportFolder(baseDir, ticketId);
    }
}
