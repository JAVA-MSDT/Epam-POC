package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.HtmlReportWriter;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.awt.Desktop;
import java.io.File;
import java.util.Scanner;

/**
 * Step 5 — Review & Clarification.
 * <p>
 * Reads:  ctx.deepDive, ctx.visualReport, ctx.htmlReportPath, ctx.reportFolderPath
 * Writes: ctx.reviewNotes, updates ctx.htmlReportPath (if report is regenerated)
 * <p>
 * Opens the HTML report in the default browser so the developer can review it.
 * Enters an iteration loop: the developer can request changes to the report or
 * approve it to proceed. Each iteration re-prompts the LLM with the feedback,
 * regenerates the HTML body, and rewrites the file on disk.
 */
public class ReviewAgent {

    private static final int MAX_ITERATIONS = 5;

    private final LlmClient llmClient;

    public ReviewAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        openReportInBrowser(ctx.getHtmlReportPath());

        String approvedHtmlBody = iterateUntilApproved(ctx);

        String reviewPrompt = MarkdownLoader.load("agents/review.md")
                .replace("{{deep_dive}}", ctx.getDeepDive())
                .replace("{{visual_report}}", approvedHtmlBody);

        ctx.setReviewNotes(llmClient.completePrompt(reviewPrompt));
    }

    private String iterateUntilApproved(WorkflowContext ctx) {
        String currentHtmlBody = ctx.getVisualReport();
        int iteration = 0;

        try (Scanner scanner = new Scanner(System.in)) {
            while (iteration < MAX_ITERATIONS) {
                System.out.println("\n[ReviewAgent] The HTML report is ready for your review.");
                if (ctx.getHtmlReportPath() != null) {
                    System.out.println("  File: " + ctx.getHtmlReportPath());
                }
                System.out.println("  Options:");
                System.out.println("    [a] Approve report and continue");
                System.out.println("    [f] Provide feedback to improve the report");
                System.out.print("  Your choice: ");

                String choice = scanner.nextLine().trim().toLowerCase();

                if ("a".equals(choice)) {
                    System.out.println("[ReviewAgent] Report approved.");
                    return currentHtmlBody;
                }

                if ("f".equals(choice)) {
                    System.out.print("  Enter your feedback: ");
                    String feedback = scanner.nextLine().trim();
                    if (feedback.isBlank()) {
                        System.out.println("  No feedback entered — please try again.");
                        continue;
                    }
                    System.out.println("[ReviewAgent] Regenerating report with your feedback...");
                    currentHtmlBody = regenerateReport(ctx, currentHtmlBody, feedback);
                    ctx.setVisualReport(currentHtmlBody);
                    rewriteHtmlFile(ctx, currentHtmlBody);
                    openReportInBrowser(ctx.getHtmlReportPath());
                    iteration++;
                } else {
                    System.out.println("  Invalid choice — please enter 'a' or 'f'.");
                }
            }
        }

        System.out.println("[ReviewAgent] Max iterations reached — proceeding with current report.");
        return currentHtmlBody;
    }

    private String regenerateReport(WorkflowContext ctx, String currentHtmlBody, String feedback) {
        String iterationPrompt = MarkdownLoader.load("agents/visual_report.md")
                .replace("{{deep_dive}}", ctx.getDeepDive())
                + "\n\n## Developer Feedback\n\nThe previous version of the report was:\n\n"
                + currentHtmlBody
                + "\n\nThe developer provided the following feedback — update the report accordingly:\n\n"
                + feedback;
        return llmClient.completePrompt(iterationPrompt);
    }

    private void rewriteHtmlFile(WorkflowContext ctx, String htmlBody) {
        String reportFolder = ctx.getReportFolderPath();
        String existing = ctx.getHtmlReportPath();
        if (existing != null && !existing.isBlank()) {
            String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "Analysis";
            HtmlReportWriter.write(existing, "Analysis Report — " + ticketId, htmlBody);
            System.out.println("[ReviewAgent] Report updated at: " + existing);
        } else if (reportFolder != null && !reportFolder.isBlank()) {
            String ticketId = ctx.getJiraTicketId() != null ? ctx.getJiraTicketId() : "Analysis";
            String path = HtmlReportWriter.write(reportFolder, "analysis_report.html",
                    "Analysis Report — " + ticketId, htmlBody);
            ctx.setHtmlReportPath(path);
            System.out.println("[ReviewAgent] Report written at: " + path);
        }
    }

    private void openReportInBrowser(String htmlReportPath) {
        if (htmlReportPath == null || htmlReportPath.isBlank()) {
            System.out.println("[ReviewAgent] No HTML report path set — open manually if needed.");
            return;
        }
        try {
            File file = new File(htmlReportPath);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                System.out.println("[ReviewAgent] Opened report in browser: " + htmlReportPath);
            } else {
                System.out.println("[ReviewAgent] Please open the report manually: " + htmlReportPath);
            }
        } catch (Exception e) {
            System.out.println("[ReviewAgent] Could not open browser automatically — open manually: " + htmlReportPath);
        }
    }
}
