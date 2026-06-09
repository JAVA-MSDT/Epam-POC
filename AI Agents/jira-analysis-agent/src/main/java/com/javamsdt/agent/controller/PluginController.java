package com.javamsdt.agent.controller;

import com.javamsdt.agent.config.plugin.OptimizedPromptPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plugins")
public class PluginController {

    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);

    private final OptimizedPromptPluginManager pluginManager;

    public PluginController(OptimizedPromptPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @GetMapping("/available")
    public ResponseEntity<List<String>> getAvailablePlugins() {
        return ResponseEntity.ok(pluginManager.getAvailablePlugins());
    }

    @PostMapping("/reload/{pluginName}")
    public ResponseEntity<Map<String, String>> reloadPlugin(@PathVariable String pluginName) {
        logger.info("Manual reload requested for plugin: {}", pluginName);
        pluginManager.reloadPlugin(pluginName);
        return ResponseEntity.ok(Map.of("status", "reloaded", "plugin", pluginName));
    }

    @PostMapping("/reload-all")
    public ResponseEntity<Map<String, String>> reloadAllPlugins() {
        logger.info("Manual reload-all requested");
        pluginManager.reloadAllPlugins();
        return ResponseEntity.ok(Map.of("status", "reloaded", "count", "all"));
    }
}
