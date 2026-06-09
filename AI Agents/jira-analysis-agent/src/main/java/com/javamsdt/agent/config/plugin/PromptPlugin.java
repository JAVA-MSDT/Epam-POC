package com.javamsdt.agent.config.plugin;

import java.time.LocalDateTime;
import java.util.Map;

public record PromptPlugin(
        String name,
        String content,
        Map<String, Object> metadata,
        LocalDateTime lastModified,
        PluginSource source
) {

    public enum PluginSource {
        EXTERNAL_FILE,
        INTERNAL_RESOURCE
    }
}
