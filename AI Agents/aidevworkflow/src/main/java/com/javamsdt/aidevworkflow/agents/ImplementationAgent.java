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
 * Step 6 — Implementation.
 *
 * Reads:  ctx.reviewNotes, ctx.projectRootPath
 * Writes: ctx.implementation (full LLM response), writes code files to disk
 *
 * The LLM is asked to produce code blocks annotated with a file path comment
 * on the first line (e.g. "// FILE: src/main/java/com/example/Foo.java").
 * This agent parses those blocks and writes each file to projectRootPath on disk.
 */
public class ImplementationAgent {

    // Matches: ```java\n// FILE: path/to/File.java\n<code>\n```
    private static final Pattern FILE_BLOCK_PATTERN = Pattern.compile(
            "```(?:java|kotlin|python|typescript|javascript|go|cs|xml|yaml|yml|properties|sh)?\\s*\\n"
            + "//\\s*FILE:\\s*([^\\n]+)\\n"
            + "(.*?)\\n```",
            Pattern.DOTALL
    );

    private final LlmClient llmClient;

    public ImplementationAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void execute(WorkflowContext ctx) {
        String projectRoot = ctx.getProjectRootPath();

        String prompt = MarkdownLoader.load("agents/implementation.md")
                .replace("{{review_notes}}", ctx.getReviewNotes())
                .replace("{{project_root}}", projectRoot != null ? projectRoot : "(not set — code blocks will not be written to disk)");

        String response = llmClient.completePrompt(prompt);
        ctx.setImplementation(response);

        if (projectRoot != null && !projectRoot.isBlank()) {
            List<String> written = writeCodeFiles(projectRoot, response);
            if (!written.isEmpty()) {
                System.out.println("[ImplementationAgent] Wrote " + written.size() + " file(s) to disk:");
                written.forEach(f -> System.out.println("  " + f));
            } else {
                System.out.println("[ImplementationAgent] No FILE: annotated blocks found — no files written.");
            }
        } else {
            System.out.println("[ImplementationAgent] No projectRootPath set — code files not written to disk.");
        }
    }

    private List<String> writeCodeFiles(String projectRoot, String response) {
        List<String> writtenPaths = new ArrayList<>();
        Matcher matcher = FILE_BLOCK_PATTERN.matcher(response);

        while (matcher.find()) {
            String relativePath = matcher.group(1).trim();
            String content = matcher.group(2);
            String fullPath = projectRoot + "/" + relativePath;

            try {
                FileSystemUtil.writeFile(fullPath, content);
                writtenPaths.add(fullPath);
            } catch (Exception e) {
                System.err.println("[ImplementationAgent] Failed to write " + fullPath + ": " + e.getMessage());
            }
        }

        return writtenPaths;
    }
}
