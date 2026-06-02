package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 2 — Project Setup.
 * Reads: ctx.ticketSummary
 * Writes: ctx.projectSetup
 */
public class ProjectSetupAgent {

    private final LlmClient llmClient;

    public ProjectSetupAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/project_setup.md")
                .replace("{{ticket_summary}}", ctx.getTicketSummary());
        ctx.setProjectSetup(llmClient.completePrompt(prompt));
    }
}
