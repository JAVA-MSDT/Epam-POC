package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.jira.JiraClient;
import com.javamsdt.aidevworkflow.jira.JiraTicket;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

/**
 * Step 1 — Ticket Analysis.
 * <p>
 * Reads:  ctx.jiraTicketId (preferred) or ctx.ticketText (fallback)
 * Writes: ctx.ticketText (populated from Jira if fetched), ctx.ticketSummary
 * <p>
 * If jiraTicketId is set, the agent fetches the full ticket from Jira and
 * stores the structured text back into ctx.ticketText before analysing it.
 * If no jiraTicketId is set, ctx.ticketText is used directly (local/test mode).
 */
public class TicketAnalysisAgent {

    private final LlmClient llmClient;
    private final JiraClient jiraClient;

    /**
     * Full constructor — Jira integration enabled.
     */
    public TicketAnalysisAgent(LlmClient llmClient, JiraClient jiraClient) {
        this.llmClient = llmClient;
        this.jiraClient = jiraClient;
    }

    /**
     * Convenience constructor — no Jira client; uses ctx.ticketText directly.
     */
    public TicketAnalysisAgent(LlmClient llmClient) {
        this(llmClient, null);
    }

    public void execute(WorkflowContext ctx) {
        String ticketContent = resolveTicketContent(ctx);

        String prompt = MarkdownLoader.load("agents/ticket_analysis.md")
                .replace("{{ticket}}", ticketContent);

        ctx.setTicketSummary(llmClient.completePrompt(prompt));
    }

    private String resolveTicketContent(WorkflowContext ctx) {
        String ticketId = ctx.getJiraTicketId();

        if (jiraClient != null && ticketId != null && !ticketId.isBlank()) {
            System.out.println("[TicketAnalysisAgent] Fetching ticket from Jira: " + ticketId);
            JiraTicket ticket = jiraClient.fetchTicket(ticketId);
            String structured = ticket.toPromptText();
            ctx.setTicketText(structured);
            return structured;
        }

        System.out.println("[TicketAnalysisAgent] No Jira ticket ID set — using ctx.ticketText directly.");
        return ctx.getTicketText();
    }
}
