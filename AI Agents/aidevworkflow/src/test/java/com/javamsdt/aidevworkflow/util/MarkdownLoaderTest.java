package com.javamsdt.aidevworkflow.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownLoaderTest {

    @Test
    void loadShouldReturnNonEmptyContent() {
        String content = MarkdownLoader.load("agents/ticket_analysis.md");
        assertNotNull(content);
        assertFalse(content.isBlank());
    }

    @Test
    void loadShouldRetainTemplatePlaceholder() {
        String content = MarkdownLoader.load("agents/ticket_analysis.md");
        assertTrue(content.contains("{{ticket}}"),
                "ticket_analysis.md must contain the {{ticket}} placeholder");
    }

    @Test
    void loadShouldLoadAllEightAgentFiles() {
        String[] files = {
                "agents/ticket_analysis.md",
                "agents/project_setup.md",
                "agents/deep_dive.md",
                "agents/visual_report.md",
                "agents/review.md",
                "agents/implementation.md",
                "agents/quality_assurance.md",
                "agents/deployment.md"
        };
        for (String file : files) {
            assertDoesNotThrow(() -> MarkdownLoader.load(file),
                    "Failed to load: " + file);
        }
    }

    @Test
    void loadShouldThrowForMissingResource() {
        assertThrows(IllegalArgumentException.class,
                () -> MarkdownLoader.load("agents/does_not_exist.md"));
    }
}
