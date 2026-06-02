package com.javamsdt.aidevworkflow.orchestrator;

import com.javamsdt.aidevworkflow.agents.*;
import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.github.GitClient;
import com.javamsdt.aidevworkflow.github.GitHubClient;
import com.javamsdt.aidevworkflow.jira.JiraClient;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.util.Scanner;

/**
 * Coordinates all 8 workflow steps with a shared WorkflowContext.
 * <p>
 * Two execution modes are available:
 * <p>
 * runWorkflow()          — full modular mode: 8 separate LLM calls, one per agent.
 * Best for debugging and transparency.
 * <p>
 * runWorkflowOptimized() — optimized mode: 5 LLM calls by combining paired steps
 * (1+2, 3+4, 6+7) into single prompts.
 * Best for production to reduce latency and cost.
 * <p>
 * Both modes insert three human-in-the-loop confirmation gates:
 * Gate 1 — after Steps 1+2  (approve project structure before deep analysis)
 * Gate 2 — after Step 5     (approve analysis before writing code)
 * Gate 3 — after Step 8     (approve deployment)
 * <p>
 * Pass autoApprove=true to skip all gates (useful for automated tests).
 */
public class WorkflowOrchestrator {

    private final LlmClient llmClient;
    private final WorkflowContext ctx;
    private final boolean autoApprove;
    private final JiraClient jiraClient;
    private final GitClient gitClient;
    private final GitHubClient gitHubClient;

    private final TicketAnalysisAgent ticketAnalysisAgent;
    private final ProjectSetupAgent projectSetupAgent;
    private final DeepDiveAgent deepDiveAgent;
    private final VisualReportAgent visualReportAgent;
    private final ReviewAgent reviewAgent;
    private final ImplementationAgent implementationAgent;
    private final QualityAssuranceAgent qualityAssuranceAgent;
    private final DeploymentAgent deploymentAgent;

    public WorkflowOrchestrator(LlmClient llmClient, WorkflowContext ctx) {
        this(llmClient, ctx, false, null, null, null);
    }

    public WorkflowOrchestrator(LlmClient llmClient, WorkflowContext ctx, boolean autoApprove) {
        this(llmClient, ctx, autoApprove, null, null, null);
    }

    public WorkflowOrchestrator(LlmClient llmClient, WorkflowContext ctx, boolean autoApprove,
                                 JiraClient jiraClient, GitClient gitClient, GitHubClient gitHubClient) {
        this.llmClient = llmClient;
        this.ctx = ctx;
        this.autoApprove = autoApprove;
        this.jiraClient = jiraClient;
        this.gitClient = gitClient;
        this.gitHubClient = gitHubClient;
        this.ticketAnalysisAgent = new TicketAnalysisAgent(llmClient, jiraClient);
        this.projectSetupAgent = new ProjectSetupAgent(llmClient);
        this.deepDiveAgent = new DeepDiveAgent(llmClient);
        this.visualReportAgent = new VisualReportAgent(llmClient);
        this.reviewAgent = new ReviewAgent(llmClient);
        this.implementationAgent = new ImplementationAgent(llmClient);
        this.qualityAssuranceAgent = new QualityAssuranceAgent(llmClient);
        this.deploymentAgent = new DeploymentAgent(llmClient, gitClient, gitHubClient);
    }

    // ══════════════════════════════════════════════════════════════
    // Full modular workflow — 8 LLM calls
    // ══════════════════════════════════════════════════════════════

    /**
     * Runs all 8 agents individually (one LLM call each).
     * Ideal for development, debugging, and per-step visibility.
     */
    public void runWorkflow() {
        log("Starting full modular workflow (8 LLM calls)...");

        log("[Step 1] Ticket Analysis");
        ticketAnalysisAgent.execute(ctx);

        log("[Step 2] Project Setup");
        projectSetupAgent.execute(ctx);

        if (!humanConfirm("Ticket Analysis & Project Setup")) {
            log("Workflow halted by user after Step 2.");
            return;
        }

        log("[Step 3] Deep Dive Analysis");
        deepDiveAgent.execute(ctx);

        log("[Step 4] Visual Analysis Report");
        visualReportAgent.execute(ctx);

        log("[Step 5] Review & Clarification");
        reviewAgent.execute(ctx);

        if (!humanConfirm("Review & Clarification")) {
            log("Workflow halted by user after Step 5.");
            return;
        }

        log("[Step 6] Implementation");
        implementationAgent.execute(ctx);

        log("[Step 7] Quality Assurance");
        qualityAssuranceAgent.execute(ctx);

        log("[Step 8] Deployment & Review — generating plan and making local commits");
        deploymentAgent.execute(ctx);

        if (humanConfirm("Deployment & Review (push branch and open PR?)")) {
            deploymentAgent.pushAndCreatePr(ctx);
        } else {
            log("Deployment halted — branch '" + ctx.getFeatureBranchName() + "' kept local, no PR created.");
        }
        log("Workflow complete.");
    }

    // ══════════════════════════════════════════════════════════════
    // Optimized workflow — 5 LLM calls
    // ══════════════════════════════════════════════════════════════

    /**
     * Runs the workflow with 5 combined LLM calls:
     * Call 1 — Steps 1+2  (Ticket Analysis + Project Setup combined)
     * Call 2 — Steps 3+4  (Deep Dive + Visual Report combined)
     * Call 3 — Step 5     (Review)
     * Call 4 — Steps 6+7  (Implementation + QA combined)
     * Call 5 — Step 8     (Deployment)
     */
    public void runWorkflowOptimized() {
        log("Starting optimized workflow (5 LLM calls)...");

        log("[Steps 1+2] Ticket Analysis & Project Setup (combined)");
        runBatchTicketAndSetup();

        if (!humanConfirm("Ticket Analysis & Project Setup")) {
            log("Workflow halted by user after Steps 1+2.");
            return;
        }

        log("[Steps 3+4] Deep Dive & Visual Report (combined)");
        runBatchDeepDiveAndVisual();

        log("[Step 5] Review & Clarification");
        reviewAgent.execute(ctx);

        if (!humanConfirm("Review & Clarification")) {
            log("Workflow halted by user after Step 5.");
            return;
        }

        log("[Steps 6+7] Implementation & QA (combined)");
        runBatchImplementationAndQA();

        log("[Step 8] Deployment & Review — generating plan and making local commits");
        deploymentAgent.execute(ctx);

        if (humanConfirm("Deployment & Review (push branch and open PR?)")) {
            deploymentAgent.pushAndCreatePr(ctx);
        } else {
            log("Deployment halted — branch '" + ctx.getFeatureBranchName() + "' kept local, no PR created.");
        }
        log("Workflow complete.");
    }

    // ══════════════════════════════════════════════════════════════
    // Combined-prompt batch helpers (used only by runWorkflowOptimized)
    // ══════════════════════════════════════════════════════════════

    private void runBatchTicketAndSetup() {
        String ticketPrompt = MarkdownLoader.load("agents/ticket_analysis.md")
                .replace("{{ticket}}", ctx.getTicketText());
        String setupPrompt = MarkdownLoader.load("agents/project_setup.md")
                .replace("{{ticket_summary}}", "[derived from the ticket analysis you produce above]");

        String combined = ticketPrompt
                + "\n\n---\n\n"
                + "## Combined Task: Project Setup\n\n"
                + "Immediately after completing the ticket analysis above, also perform the "
                + "following project setup task using your ticket analysis as input:\n\n"
                + setupPrompt
                + "\n\n---\n\n"
                + "Label your response sections exactly as:\n"
                + "## TICKET_ANALYSIS\n[ticket analysis output here]\n\n"
                + "## PROJECT_SETUP\n[project setup output here]";

        String response = llmClient.completePrompt(combined);
        ctx.setTicketSummary(extractSection(response, "TICKET_ANALYSIS"));
        ctx.setProjectSetup(extractSection(response, "PROJECT_SETUP"));
    }

    private void runBatchDeepDiveAndVisual() {
        String deepDivePrompt = MarkdownLoader.load("agents/deep_dive.md")
                .replace("{{ticket_summary}}", ctx.getTicketSummary())
                .replace("{{project_setup}}", ctx.getProjectSetup());
        String visualPrompt = MarkdownLoader.load("agents/visual_report.md")
                .replace("{{deep_dive}}", "[derived from the deep dive analysis you produce above]");

        String combined = deepDivePrompt
                + "\n\n---\n\n"
                + "## Combined Task: Visual Analysis Report\n\n"
                + "Immediately after your deep dive analysis, also produce the following "
                + "visual report using your deep dive as input:\n\n"
                + visualPrompt
                + "\n\n---\n\n"
                + "Label your response sections exactly as:\n"
                + "## DEEP_DIVE\n[deep dive output here]\n\n"
                + "## VISUAL_REPORT\n[visual report output here]";

        String response = llmClient.completePrompt(combined);
        ctx.setDeepDive(extractSection(response, "DEEP_DIVE"));
        ctx.setVisualReport(extractSection(response, "VISUAL_REPORT"));
    }

    private void runBatchImplementationAndQA() {
        String implPrompt = MarkdownLoader.load("agents/implementation.md")
                .replace("{{review_notes}}", ctx.getReviewNotes());
        String qaPrompt = MarkdownLoader.load("agents/quality_assurance.md")
                .replace("{{implementation}}", "[derived from the implementation plan you produce above]");

        String combined = implPrompt
                + "\n\n---\n\n"
                + "## Combined Task: Quality Assurance\n\n"
                + "Immediately after producing the implementation plan above, also perform "
                + "the following QA review using your implementation plan as input:\n\n"
                + qaPrompt
                + "\n\n---\n\n"
                + "Label your response sections exactly as:\n"
                + "## IMPLEMENTATION\n[implementation output here]\n\n"
                + "## QA_REPORT\n[QA report output here]";

        String response = llmClient.completePrompt(combined);
        ctx.setImplementation(extractSection(response, "IMPLEMENTATION"));
        ctx.setQaReport(extractSection(response, "QA_REPORT"));
    }

    // ══════════════════════════════════════════════════════════════
    // Re-run helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Clears the cached codebase snapshot and re-runs Steps 3+4 (Deep Dive + Visual Report).
     * <p>
     * Use this after the codebase has changed (e.g., after implementation) so the analysis
     * reflects the latest files on disk. Review notes, implementation, and QA results are
     * intentionally preserved — only the analysis and visual report are refreshed.
     */
    public void rerunAnalysis() {
        log("Re-running analysis with fresh codebase scan...");
        ctx.setCodebaseSnapshot(null);

        log("[Step 3] Deep Dive Analysis (re-run)");
        deepDiveAgent.execute(ctx);

        log("[Step 4] Visual Analysis Report (re-run)");
        visualReportAgent.execute(ctx);

        log("Analysis re-run complete.");
    }

    // ══════════════════════════════════════════════════════════════
    // Utilities
    // ══════════════════════════════════════════════════════════════

    /**
     * Extracts the content under a "## HEADER" section from an LLM response.
     * Falls back to the full response if the marker is not found.
     */
    private String extractSection(String text, String header) {
        String marker = "## " + header;
        int start = text.indexOf(marker);
        if (start == -1) return text;
        start = text.indexOf('\n', start);
        if (start == -1) return "";
        start++;
        int end = text.indexOf("\n## ", start);
        return (end == -1 ? text.substring(start) : text.substring(start, end)).trim();
    }

    /**
     * Human-in-the-loop confirmation gate.
     * Returns true to continue the workflow, false to halt.
     * When autoApprove is true (e.g., in tests) always returns true.
     */
    boolean humanConfirm(String stepName) {
        if (autoApprove) return true;
        System.out.printf("%n[HUMAN GATE] Step '%s' completed.%n", stepName);
        System.out.print("Review the context and approve to continue (y/n): ");
        try (Scanner scanner = new Scanner(System.in)) {
            return scanner.nextLine().trim().equalsIgnoreCase("y");
        }
    }

    private void log(String message) {
        System.out.println("[Orchestrator] " + message);
    }

    public WorkflowContext getContext() {
        return ctx;
    }
}
