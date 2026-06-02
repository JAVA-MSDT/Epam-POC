package com.javamsdt.aidevworkflow.orchestrator;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowOrchestratorTest {

    @Mock
    private LlmClient llmClient;

    private WorkflowContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new WorkflowContext();
        ctx.setTicketText("Build a REST API for user management");
        // Stub every LLM call with a non-null response so agents don't NPE
        when(llmClient.completePrompt(anyString())).thenReturn("mock LLM response");
    }

    @Test
    void runWorkflowShouldMakeEightLlmCalls() {
        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(llmClient, ctx, true);
        orchestrator.runWorkflow();

        verify(llmClient, times(8)).completePrompt(anyString());
    }

    @Test
    void runWorkflowOptimizedShouldMakeFiveLlmCalls() {
        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(llmClient, ctx, true);
        orchestrator.runWorkflowOptimized();

        verify(llmClient, times(5)).completePrompt(anyString());
    }

    @Test
    void runWorkflowShouldPopulateAllContextFields() {
        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(llmClient, ctx, true);
        orchestrator.runWorkflow();

        assertNotNull(ctx.getTicketSummary(), "ticketSummary must be set after Step 1");
        assertNotNull(ctx.getProjectSetup(), "projectSetup must be set after Step 2");
        assertNotNull(ctx.getDeepDive(), "deepDive must be set after Step 3");
        assertNotNull(ctx.getVisualReport(), "visualReport must be set after Step 4");
        assertNotNull(ctx.getReviewNotes(), "reviewNotes must be set after Step 5");
        assertNotNull(ctx.getImplementation(), "implementation must be set after Step 6");
        assertNotNull(ctx.getQaReport(), "qaReport must be set after Step 7");
        assertNotNull(ctx.getDeploymentStatus(), "deploymentStatus must be set after Step 8");
    }

    @Test
    void runWorkflowOptimizedShouldPopulateAllContextFields() {
        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(llmClient, ctx, true);
        orchestrator.runWorkflowOptimized();

        assertNotNull(ctx.getTicketSummary());
        assertNotNull(ctx.getProjectSetup());
        assertNotNull(ctx.getDeepDive());
        assertNotNull(ctx.getVisualReport());
        assertNotNull(ctx.getReviewNotes());
        assertNotNull(ctx.getImplementation());
        assertNotNull(ctx.getQaReport());
        assertNotNull(ctx.getDeploymentStatus());
    }

    @Test
    void humanConfirmShouldReturnTrueWhenAutoApproveIsEnabled() {
        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(llmClient, ctx, true);
        assertTrue(orchestrator.humanConfirm("Test Step"));
    }
}
