package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 6 — Implementation.
 * Reads: ctx.reviewNotes
 * Writes: ctx.implementation
 */
public class ImplementationAgent {

    private final LlmClient llmClient;

    public ImplementationAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/implementation.md")
                .replace("{{review_notes}}", ctx.getReviewNotes());
        ctx.setImplementation(llmClient.completePrompt(prompt));
    }
}
