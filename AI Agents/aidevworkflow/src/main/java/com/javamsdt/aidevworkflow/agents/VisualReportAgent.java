package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.HtmlReportWriter;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 4 — Visual Analysis Report.
 * <p>
 * Reads:  ctx.deepDive, ctx.reportFolderPath
 * Writes: ctx.visualReport (HTML body content), ctx.htmlReportPath (file on disk)
 * <p>
 * The LLM generates the HTML body content for the report. This agent then writes
 * it to a full HTML file in the report folder created by ProjectSetupAgent.
 */
public class VisualReportAgent {

    private static final String REPORT_FILE_NAME = "analysis_report.html";

    private final LlmClient llmClient;

    public VisualReportAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/visual_report.md")
                .replace("{{deep_dive}}", ctx.getDeepDive());

        String htmlBody = llmClient.completePrompt(prompt);
        ctx.setVisualReport(htmlBody);

        String reportFolder = ctx.getReportFolderPath();
        if (reportFolder != null && !reportFolder.isBlank()) {
            String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "Analysis";
            String title = "Analysis Report — " + ticketId;
            String htmlPath = HtmlReportWriter.write(reportFolder, REPORT_FILE_NAME, title, htmlBody);
            ctx.setHtmlReportPath(htmlPath);
            System.out.println("[VisualReportAgent] HTML report written to: " + htmlPath);
        } else {
            System.out.println("[VisualReportAgent] No reportFolderPath set — HTML report not written to disk.");
        }
    }
}
