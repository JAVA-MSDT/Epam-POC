package com.javamsdt.aidevworkflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.javamsdt.aidevworkflow.context.WorkflowContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Saves and loads a {@link WorkflowContext} as a JSON file using Jackson.
 * <p>
 * Typical usage:
 * <pre>
 *   // save after each step
 *   WorkflowContextSerializer.save(ctx, "/tmp/reports/workflow_context.json");
 *
 *   // resume in a later session
 *   WorkflowContext ctx = WorkflowContextSerializer.load("/tmp/reports/workflow_context.json");
 * </pre>
 * <p>
 * The JSON file is human-readable (pretty-printed). All fields of {@link WorkflowContext}
 * are persisted, including large ones like {@code codebaseSnapshot} and {@code implementation}.
 * Delete or null-out those fields before saving if token size is a concern.
 */
public class WorkflowContextSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String DEFAULT_FILENAME = "workflow_context.json";

    /**
     * Serializes {@code ctx} to a JSON file at {@code filePath}.
     * Parent directories are created if they do not exist.
     *
     * @param ctx      the context to persist; must not be null
     * @param filePath absolute or relative path to the output file
     * @throws RuntimeException wrapping any {@link IOException}
     */
    public static void save(WorkflowContext ctx, String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            MAPPER.writeValue(new File(filePath), ctx);
            System.out.println("[WorkflowContextSerializer] Context saved to: " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save WorkflowContext to " + filePath, e);
        }
    }

    /**
     * Deserializes a {@link WorkflowContext} from the JSON file at {@code filePath}.
     *
     * @param filePath path to an existing JSON file previously written by {@link #save}
     * @return the restored context
     * @throws RuntimeException wrapping any {@link IOException} or parse error
     */
    public static WorkflowContext load(String filePath) {
        try {
            WorkflowContext ctx = MAPPER.readValue(new File(filePath), WorkflowContext.class);
            System.out.println("[WorkflowContextSerializer] Context loaded from: " + filePath);
            return ctx;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WorkflowContext from " + filePath, e);
        }
    }

    /**
     * Convenience overload — saves to {@code <reportFolderPath>/workflow_context.json}.
     * Does nothing if {@code ctx.getReportFolderPath()} is null or blank.
     *
     * @param ctx the context to persist
     */
    public static void saveToReportFolder(WorkflowContext ctx) {
        String folder = ctx.getReportFolderPath();
        if (folder == null || folder.isBlank()) {
            System.out.println("[WorkflowContextSerializer] reportFolderPath not set — skipping auto-save.");
            return;
        }
        save(ctx, folder + "/" + DEFAULT_FILENAME);
    }
}
