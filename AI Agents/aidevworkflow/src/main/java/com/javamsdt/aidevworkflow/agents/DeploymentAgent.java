package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 8 — Deployment & Review.
 * Reads: ctx.implementation, ctx.qaReport
 * Writes: ctx.deploymentStatus
 */
public class DeploymentAgent {

    private final LlmClient llmClient;

    public DeploymentAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String prompt = MarkdownLoader.load("agents/deployment.md")
                .replace("{{implementation}}", ctx.getImplementation())
                .replace("{{qa_report}}", ctx.getQaReport());
        ctx.setDeploymentStatus(llmClient.completePrompt(prompt));
    }
}
