package com.javamsdt.aidevworkflow.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads markdown agent prompt templates from the classpath.
 * <p>
 * Usage:
 * String prompt = MarkdownLoader.load("agents/ticket_analysis.md")
 * .replace("{{ticket}}", ticketText);
 * <p>
 * Files are resolved relative to the classpath root, so place them under
 * src/main/resources/ and reference them without a leading slash.
 */
public final class MarkdownLoader {

    private MarkdownLoader() {
    }

    /**
     * Loads the content of a classpath resource as a UTF-8 string.
     *
     * @param resourcePath path relative to the classpath root
     *                     (e.g. "agents/ticket_analysis.md")
     * @return the file contents as a string
     * @throws IllegalArgumentException if the resource does not exist
     * @throws RuntimeException         if the resource cannot be read
     */
    public static String load(String resourcePath) {
        InputStream is = MarkdownLoader.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IllegalArgumentException(
                    "Classpath resource not found: " + resourcePath);
        }

        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read classpath resource: " + resourcePath, e);
        }
    }
}
