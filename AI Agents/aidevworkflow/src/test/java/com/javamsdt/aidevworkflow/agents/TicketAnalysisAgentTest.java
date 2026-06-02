package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketAnalysisAgentTest {

    @Mock
    private LlmClient llmClient;

    @Test
    void executeShouldWriteLlmResponseToTicketSummary() {
        when(llmClient.completePrompt(anyString())).thenReturn("### Summary\nUser login feature.");

        WorkflowContext ctx = new WorkflowContext();
        ctx.setTicketText("Build a login page with OAuth2 support");

        new TicketAnalysisAgent(llmClient).execute(ctx);

        assertEquals("### Summary\nUser login feature.", ctx.getTicketSummary());
    }

    @Test
    void executeShouldInjectTicketTextIntoPrompt() {
        when(llmClient.completePrompt(anyString())).thenReturn("response");

        WorkflowContext ctx = new WorkflowContext();
        ctx.setTicketText("unique_ticket_content_xyz_123");

        new TicketAnalysisAgent(llmClient).execute(ctx);

        verify(llmClient).completePrompt(contains("unique_ticket_content_xyz_123"));
    }

    @Test
    void executeShouldMakeExactlyOneLlmCall() {
        when(llmClient.completePrompt(anyString())).thenReturn("response");

        WorkflowContext ctx = new WorkflowContext();
        ctx.setTicketText("some ticket");

        new TicketAnalysisAgent(llmClient).execute(ctx);

        verify(llmClient, times(1)).completePrompt(anyString());
    }

    @Test
    void executeShouldNotModifyOtherContextFields() {
        when(llmClient.completePrompt(anyString())).thenReturn("summary");

        WorkflowContext ctx = new WorkflowContext();
        ctx.setTicketText("some ticket");

        new TicketAnalysisAgent(llmClient).execute(ctx);

        assertNull(ctx.getProjectSetup(), "projectSetup should remain null after Step 1");
        assertNull(ctx.getDeepDive(), "deepDive should remain null after Step 1");
    }
}
