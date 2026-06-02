package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 7 — Quality Assurance.
 * Reads: ctx.implementation
 * Writes: ctx.qaReport
 */
public class QualityAssuranceAgent {

    private final LlmClient llmClient;

    public QualityAssuranceAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/quality_assurance.md")
                .replace("{{implementation}}", ctx.getImplementation());
        ctx.setQaReport(llmClient.completePrompt(prompt));
    }
}
