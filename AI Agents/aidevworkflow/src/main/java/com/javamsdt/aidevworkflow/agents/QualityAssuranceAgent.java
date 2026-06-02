package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.FileSystemUtil;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.util.List;

/**
 * Step 7 — Quality Assurance.
 *
 * Reads:  ctx.implementation, ctx.projectRootPath
 * Writes: ctx.qaReport
 *
 * When projectRootPath is set, this agent reads the actual code files written to disk
 * by ImplementationAgent and includes them in the QA prompt, so the LLM reviews the
 * real committed code rather than the plan text.
 */
public class QualityAssuranceAgent {

    private static final List<String> CODE_EXTENSIONS = List.of(
            ".java", ".kt", ".py", ".ts", ".js", ".go", ".cs"
    );
    private static final int MAX_FILES = 30;

    private final LlmClient llmClient;

    public QualityAssuranceAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String codeContext = resolveCodeContext(ctx);

        String prompt = MarkdownLoader.load("agents/quality_assurance.md")
                .replace("{{implementation}}", ctx.getImplementation())
                .replace("{{written_code}}", codeContext);

        ctx.setQaReport(llmClient.completePrompt(prompt));
    }

    private String resolveCodeContext(WorkflowContext ctx) {
        if (ctx.getCodebaseSnapshot() != null && !ctx.getCodebaseSnapshot().isBlank()) {
            System.out.println("[QualityAssuranceAgent] Reusing cached codebase snapshot (" + ctx.getCodebaseSnapshot().length() + " chars).");
            return ctx.getCodebaseSnapshot();
        }
        String projectRoot = ctx.getProjectRootPath();
        if (projectRoot == null || projectRoot.isBlank()) {
            System.out.println("[QualityAssuranceAgent] No projectRootPath — reviewing implementation plan text only.");
            return "(no project root set — reviewing the implementation plan text above)";
        }
        System.out.println("[QualityAssuranceAgent] Reading written code from: " + projectRoot);
        String files = FileSystemUtil.readSourceFiles(projectRoot, CODE_EXTENSIONS, MAX_FILES);
        if (!files.isBlank()) {
            ctx.setCodebaseSnapshot(files);
        }
        return files.isBlank()
                ? "(no source files found under " + projectRoot + ")"
                : files;
    }
}
