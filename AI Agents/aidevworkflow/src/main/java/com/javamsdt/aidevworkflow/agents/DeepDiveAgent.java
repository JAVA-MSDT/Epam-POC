package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.FileSystemUtil;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.util.List;

/**
 * Step 3 — AI-Powered Deep Dive.
 *
 * Reads:  ctx.ticketSummary, ctx.projectSetup, ctx.projectRootPath (optional)
 * Writes: ctx.deepDive
 *
 * When projectRootPath is set, the agent scans the project source files and includes
 * relevant code context in the prompt so the LLM can compare the existing codebase
 * against the ticket requirements and identify gaps or conflicts.
 */
public class DeepDiveAgent {

    private static final List<String> SOURCE_EXTENSIONS = List.of(
            ".java", ".kt", ".py", ".ts", ".js", ".go", ".cs", ".xml", ".yaml", ".yml", ".properties"
    );
    private static final int MAX_SOURCE_FILES = 40;
    private static final String NO_CODEBASE = "(no project root path configured — codebase scan skipped)";

    private final LlmClient llmClient;

    public DeepDiveAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String codebaseContext = scanCodebase(ctx);

        String prompt = MarkdownLoader.load("agents/deep_dive.md")
                .replace("{{ticket_summary}}", ctx.getTicketSummary())
                .replace("{{project_setup}}", ctx.getProjectSetup())
                .replace("{{codebase_context}}", codebaseContext);

        ctx.setDeepDive(llmClient.completePrompt(prompt));
    }

    private String scanCodebase(WorkflowContext ctx) {
        if (ctx.getCodebaseSnapshot() != null && !ctx.getCodebaseSnapshot().isBlank()) {
            System.out.println("[DeepDiveAgent] Reusing cached codebase snapshot (" + ctx.getCodebaseSnapshot().length() + " chars).");
            return ctx.getCodebaseSnapshot();
        }
        String rootPath = ctx.getProjectRootPath();
        if (rootPath == null || rootPath.isBlank()) {
            System.out.println("[DeepDiveAgent] No projectRootPath set — skipping codebase scan.");
            return NO_CODEBASE;
        }
        System.out.println("[DeepDiveAgent] Scanning codebase at: " + rootPath);
        String files = FileSystemUtil.readSourceFiles(rootPath, SOURCE_EXTENSIONS, MAX_SOURCE_FILES);
        if (files.isBlank()) {
            return "(codebase scan found no matching source files under: " + rootPath + ")";
        }
        ctx.setCodebaseSnapshot(files);
        System.out.println("[DeepDiveAgent] Codebase snapshot cached (" + files.length() + " chars).");
        return files;
    }
}
