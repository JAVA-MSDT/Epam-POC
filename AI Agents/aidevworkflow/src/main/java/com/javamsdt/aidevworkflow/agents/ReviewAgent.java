package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 5 — Review & Clarification.
 * Reads: ctx.deepDive, ctx.visualReport
 * Writes: ctx.reviewNotes
 */
public class ReviewAgent {

    private final LlmClient llmClient;

    public ReviewAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/review.md")
                .replace("{{deep_dive}}", ctx.getDeepDive())
                .replace("{{visual_report}}", ctx.getVisualReport());
        ctx.setReviewNotes(llmClient.completePrompt(prompt));
    }
}
