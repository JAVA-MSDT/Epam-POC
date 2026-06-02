package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 3 — AI-Powered Deep Dive.
 * Reads: ctx.ticketSummary, ctx.projectSetup
 * Writes: ctx.deepDive
 */
public class DeepDiveAgent {

    private final LlmClient llmClient;

    public DeepDiveAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/deep_dive.md")
                .replace("{{ticket_summary}}", ctx.getTicketSummary())
                .replace("{{project_setup}}", ctx.getProjectSetup());
        ctx.setDeepDive(llmClient.completePrompt(prompt));
    }
}
