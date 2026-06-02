package com.javamsdt.aidevworkflow;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.ClaudeApiClient;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.llm.OllamaApiClient;
import com.javamsdt.aidevworkflow.orchestrator.WorkflowOrchestrator;

/**
 * Entry point — demonstrates both workflow execution modes.
 * <p>
 * Prerequisites:
 * export ANTHROPIC_API_KEY=your_key_here
 * <p>
 * Run:
 * mvn compile exec:java -Dexec.mainClass="com.javamsdt.aidevworkflow.Main"
 * <p>
 * To switch to OpenAI, replace ClaudeApiClient.fromEnv() with
 * OpenAiApiClient.fromEnv() and set OPENAI_API_KEY instead.
 */
public class Main {

    private static final String SAMPLE_TICKET = """
            As a user I want to be able to log in to the application using my email
            and password so that I can access my personal dashboard.
            
            Acceptance criteria:
            - Login form with email and password fields
            - JWT-based session management (token expires in 24 hours)
            - Rate limiting: max 5 failed attempts before 15-minute lockout
            - Passwords must be stored as bcrypt hashes
            - Redirect to /dashboard on success, show inline error on failure
            """;

    public static void main(String[] args) {
        LlmClient llmClient = OllamaApiClient.fromEnv();

        WorkflowContext ctx = new WorkflowContext();
        ctx.setTicketText(SAMPLE_TICKET);

        // Choose mode via first argument: "optimized" or default (full)
        boolean optimized = args.length > 0 && args[0].equalsIgnoreCase("optimized");

        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(llmClient, ctx);

        if (optimized) {
            System.out.println("Mode: Optimized (5 LLM calls)");
            orchestrator.runWorkflowOptimized();
        } else {
            System.out.println("Mode: Full modular (8 LLM calls)");
            orchestrator.runWorkflow();
        }

        System.out.println("\n=== Final Context ===");
        System.out.println(ctx);
    }
}
