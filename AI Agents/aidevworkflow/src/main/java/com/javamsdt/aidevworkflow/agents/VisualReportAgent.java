package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 4 — Visual Analysis Report.
 * Reads: ctx.deepDive
 * Writes: ctx.visualReport
 */
public class VisualReportAgent {

    private final LlmClient llmClient;

    public VisualReportAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/visual_report.md")
                .replace("{{deep_dive}}", ctx.getDeepDive());
        ctx.setVisualReport(llmClient.completePrompt(prompt));
    }
}
