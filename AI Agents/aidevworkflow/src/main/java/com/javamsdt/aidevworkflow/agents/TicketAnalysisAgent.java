package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 1 — Ticket Analysis.
 * Reads: ctx.ticketText
 * Writes: ctx.ticketSummary
 */
public class TicketAnalysisAgent {

    private final LlmClient llmClient;

    public TicketAnalysisAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/ticket_analysis.md")
                .replace("{{ticket}}", ctx.getTicketText());
        ctx.setTicketSummary(llmClient.completePrompt(prompt));
    }
}
