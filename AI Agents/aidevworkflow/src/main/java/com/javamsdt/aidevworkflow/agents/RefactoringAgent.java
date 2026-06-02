package com.javamsdt.aidevworkflow.agents;

import com.javamsdt.aidevworkflow.context.WorkflowContext;
import com.javamsdt.aidevworkflow.llm.LlmClient;
import com.javamsdt.aidevworkflow.util.FileSystemUtil;
import com.javamsdt.aidevworkflow.util.MarkdownLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional step — Refactoring (runs between Steps 7 and 8).
 *
 * Reads:  ctx.implementation, ctx.qaReport, ctx.codebaseSnapshot (or scans if null)
 * Writes: ctx.refactoringPlan, ctx.writtenFiles (appends refactored files)
 *
 * Not part of the core 8-step pipeline; invoke via
 * {@link com.javamsdt.aidevworkflow.orchestrator.WorkflowOrchestrator#runRefactoring()}.
 */
public class RefactoringAgent {

    private static final Pattern FILE_BLOCK_PATTERN = Pattern.compile(
            "```(?:java|kotlin|python|typescript|javascript|go|cs|xml|yaml|yml|properties|sh)?\\s*\\n"
            + "//\\s*FILE:\\s*([^\\n]+)\\n"
            + "(.*?)\\n```",
            Pattern.DOTALL
    );

    private static final List<String> SOURCE_EXTENSIONS = List.of(
            ".java", ".kt", ".py", ".ts", ".js", ".go", ".cs", ".xml", ".yaml", ".yml", ".properties"
    );
    private static final int MAX_FILES = 40;

    private final LlmClient llmClient;

    public RefactoringAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Generates and applies refactoring changes to the project.
     *
     * @param ctx shared workflow context; codebaseSnapshot is used if populated,
     *            otherwise projectRootPath is scanned and the snapshot is cached
     */
    public void execute(WorkflowContext ctx) {
        String codebaseContext = resolveCodebaseContext(ctx);

        String prompt = MarkdownLoader.load("agents/refactoring.md")
                .replace("{{implementation}}", ctx.getImplementation() != null ? ctx.getImplementation() : "(none)")
                .replace("{{qa_report}}", ctx.getQaReport() != null ? ctx.getQaReport() : "(none)")
                .replace("{{codebase_snapshot}}", codebaseContext);

        String response = llmClient.completePrompt(prompt);
        ctx.setRefactoringPlan(response);

        String projectRoot = ctx.getProjectRootPath();
        if (projectRoot != null && !projectRoot.isBlank()) {
            List<String> written = writeRefactoredFiles(projectRoot, response);
            if (!written.isEmpty()) {
                ctx.getWrittenFiles().addAll(written);
                ctx.setCodebaseSnapshot(null);
                System.out.println("[RefactoringAgent] Wrote " + written.size() + " refactored file(s); snapshot cleared for re-scan.");
            } else {
                System.out.println("[RefactoringAgent] No FILE: annotated blocks in response — no files written.");
            }
        }
    }

    private String resolveCodebaseContext(WorkflowContext ctx) {
        if (ctx.getCodebaseSnapshot() != null && !ctx.getCodebaseSnapshot().isBlank()) {
            System.out.println("[RefactoringAgent] Reusing cached codebase snapshot (" + ctx.getCodebaseSnapshot().length() + " chars).");
            return ctx.getCodebaseSnapshot();
        }
        String root = ctx.getProjectRootPath();
        if (root == null || root.isBlank()) {
            System.out.println("[RefactoringAgent] No projectRootPath — refactoring from plan text only.");
            return "(no project root set — refactoring the implementation plan text above)";
        }
        System.out.println("[RefactoringAgent] Scanning codebase at: " + root);
        String files = FileSystemUtil.readSourceFiles(root, SOURCE_EXTENSIONS, MAX_FILES);
        if (!files.isBlank()) {
            ctx.setCodebaseSnapshot(files);
        }
        return files.isBlank() ? "(no source files found under " + root + ")" : files;
    }

    private List<String> writeRefactoredFiles(String projectRoot, String response) {
        List<String> written = new ArrayList<>();
        Matcher matcher = FILE_BLOCK_PATTERN.matcher(response);
        while (matcher.find()) {
            String relativePath = matcher.group(1).trim();
            String content = matcher.group(2);
            String fullPath = projectRoot + "/" + relativePath;
            try {
                FileSystemUtil.writeFile(fullPath, content);
                written.add(fullPath);
            } catch (Exception e) {
                System.err.println("[RefactoringAgent] Failed to write " + fullPath + ": " + e.getMessage());
            }
        }
        return written;
    }
}
