package com.javamsdt.agent.config.plugin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OptimizedPromptPluginManager implements PluginManager<PromptPlugin> {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedPromptPluginManager.class);

    private final String externalConfigPath;
    private final PluginLoadingStrategy strategy;
    private final boolean hotReloadEnabled;

    private final LoadingCache<String, PromptPlugin> pluginCache;
    private final Cache<String, LocalDateTime> lastModifiedCache;

    private DirectoryWatcher directoryWatcher;
    private final Set<String> watchedDirectories = ConcurrentHashMap.newKeySet();

    public OptimizedPromptPluginManager(
            @Value("${agent.plugins.external-config-path:./external-config}") String externalConfigPath,
            @Value("${agent.plugins.strategy:EXTERNAL_FIRST}") PluginLoadingStrategy strategy,
            @Value("${agent.plugins.hot-reload.enabled:true}") boolean hotReloadEnabled) {

        this.externalConfigPath = externalConfigPath;
        this.strategy = strategy;
        this.hotReloadEnabled = hotReloadEnabled;

        this.pluginCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .refreshAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build(this::loadPluginFromSource);

        this.lastModifiedCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(2))
                .build();
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing Optimized Prompt Plugin Manager");
        preWarmCache();
        if (hotReloadEnabled) {
            setupFileWatching();
        }
        logger.info("Plugin Manager initialized. Cache stats: {}", pluginCache.stats());
    }

    @Override
    public PromptPlugin loadPlugin(String name) {
        return loadPlugin(name, Map.of());
    }

    @Override
    public PromptPlugin loadPlugin(String name, Map<String, Object> context) {
        logger.debug("Loading plugin: {} (cache hit rate: {})", name, pluginCache.stats().hitRate());

        try {
            PromptPlugin plugin = pluginCache.get(name);

            if (plugin != null && !context.isEmpty()) {
                String processedContent = processTemplate(plugin.content(), context);
                return new PromptPlugin(
                        plugin.name(),
                        processedContent,
                        plugin.metadata(),
                        plugin.lastModified(),
                        plugin.source()
                );
            }

            return plugin;

        } catch (Exception e) {
            logger.error("Error loading plugin: {}", name, e);
            return null;
        }
    }

    private PromptPlugin loadPluginFromSource(String name) {
        logger.debug("Loading plugin from source: {} with strategy: {}", name, strategy);

        return switch (strategy) {
            case EXTERNAL_FIRST -> loadExternalFirst(name);
            case INTERNAL_FIRST -> loadInternalFirst(name);
            case EXTERNAL_ONLY -> loadExternalOnly(name);
        };
    }

    private PromptPlugin loadExternalFirst(String name) {
        PromptPlugin external = loadFromExternal(name);
        if (external != null) {
            logger.debug("Loaded external plugin: {}", name);
            return external;
        }

        PromptPlugin internal = loadFromInternal(name);
        if (internal != null) {
            logger.debug("Loaded internal plugin: {} (external not found)", name);
            return internal;
        }

        logger.warn("Plugin not found: {}", name);
        return null;
    }

    private PromptPlugin loadInternalFirst(String name) {
        PromptPlugin internal = loadFromInternal(name);
        if (internal != null) {
            logger.debug("Loaded internal plugin: {}", name);
            return internal;
        }

        PromptPlugin external = loadFromExternal(name);
        if (external != null) {
            logger.debug("Loaded external plugin: {} (internal not found)", name);
            return external;
        }

        logger.warn("Plugin not found: {}", name);
        return null;
    }

    private PromptPlugin loadExternalOnly(String name) {
        PromptPlugin external = loadFromExternal(name);
        if (external != null) {
            logger.debug("Loaded external plugin: {}", name);
            return external;
        }

        logger.warn("External plugin not found: {}", name);
        return null;
    }

    private PromptPlugin loadFromExternal(String name) {
        try {
            Path externalFile = Paths.get(externalConfigPath, "prompts", name + ".md");
            if (Files.exists(externalFile)) {
                LocalDateTime fileModified = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(externalFile).toInstant(),
                        java.time.ZoneId.systemDefault()
                );

                String content = Files.readString(externalFile);
                lastModifiedCache.put(name, fileModified);

                return new PromptPlugin(
                        name,
                        content,
                        extractMetadata(externalFile),
                        fileModified,
                        PromptPlugin.PluginSource.EXTERNAL_FILE
                );
            }
        } catch (IOException e) {
            logger.error("Error loading external plugin: {}", name, e);
        }
        return null;
    }

    private PromptPlugin loadFromInternal(String name) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/default/" + name + ".md");
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes());

                return new PromptPlugin(
                        name,
                        content,
                        Map.of(
                                "source", "internal",
                                "resource", resource.getFilename(),
                                "path", resource.getPath()
                        ),
                        LocalDateTime.now(),
                        PromptPlugin.PluginSource.INTERNAL_RESOURCE
                );
            }
        } catch (IOException e) {
            logger.error("Error loading internal plugin: {}", name, e);
        }
        return null;
    }

    private void preWarmCache() {
        logger.info("Pre-warming plugin cache...");

        List<String> defaultPlugins = List.of(
                "analysis-prompt",
                "code-review-prompt",
                "risk-assessment-prompt",
                "effort-estimation-prompt"
        );

        CompletableFuture.allOf(
                defaultPlugins.stream()
                        .map(pluginName -> CompletableFuture.runAsync(() -> {
                            try {
                                pluginCache.get(pluginName);
                                logger.debug("Pre-warmed plugin: {}", pluginName);
                            } catch (Exception e) {
                                logger.warn("Failed to pre-warm plugin: {}", pluginName, e);
                            }
                        }))
                        .toArray(CompletableFuture[]::new)
        ).join();

        logger.info("Cache pre-warming completed. Loaded {} plugins", pluginCache.estimatedSize());
    }

    private void setupFileWatching() {
        try {
            Path watchPath = Paths.get(externalConfigPath);
            if (!Files.exists(watchPath)) {
                Files.createDirectories(watchPath);
            }

            this.directoryWatcher = DirectoryWatcher.builder()
                    .path(watchPath)
                    .listener(this::handleFileChange)
                    .build();

            CompletableFuture.runAsync(() -> {
                try {
                    directoryWatcher.watch();
                } catch (RuntimeException e) {
                    logger.error("Error watching directory: {}", watchPath, e);
                }
            });

            logger.info("File watching enabled for: {}", watchPath);

        } catch (IOException e) {
            logger.error("Failed to setup file watching", e);
        }
    }

    private void handleFileChange(DirectoryChangeEvent event) {
        Path changedFile = event.path();
        String fileName = changedFile.getFileName().toString();

        if (fileName.endsWith(".md") && changedFile.toString().contains("prompts")) {
            String pluginName = fileName.replace(".md", "");

            logger.info("Detected file change: {} ({})", pluginName, event.eventType());

            pluginCache.invalidate(pluginName);
            lastModifiedCache.invalidate(pluginName);

            CompletableFuture.runAsync(() -> {
                try {
                    pluginCache.get(pluginName);
                    logger.info("Reloaded plugin: {}", pluginName);
                } catch (Exception e) {
                    logger.error("Failed to reload plugin: {}", pluginName, e);
                }
            });
        }
    }

    private String processTemplate(String content, Map<String, Object> context) {
        if (context.isEmpty()) {
            return content;
        }

        String processed = content;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }
        return processed;
    }

    private Map<String, Object> extractMetadata(Path file) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file-path", file.toString());
        metadata.put("last-modified", Files.getLastModifiedTime(file).toInstant());
        metadata.put("size", Files.size(file));
        metadata.put("readable", Files.isReadable(file));
        return metadata;
    }

    @Override
    public void reloadPlugin(String name) {
        logger.info("Manually reloading plugin: {}", name);
        pluginCache.invalidate(name);
        lastModifiedCache.invalidate(name);

        CompletableFuture.runAsync(() -> {
            try {
                pluginCache.get(name);
                logger.info("Plugin reloaded: {}", name);
            } catch (Exception e) {
                logger.error("Failed to reload plugin: {}", name, e);
            }
        });
    }

    @Override
    public void reloadAllPlugins() {
        logger.info("Reloading all plugins");
        pluginCache.invalidateAll();
        lastModifiedCache.invalidateAll();
        preWarmCache();
    }

    @Override
    public List<String> getAvailablePlugins() {
        Set<String> plugins = new HashSet<>();

        try {
            var resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:prompts/default/*.md");
            for (var resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    plugins.add(filename.replace(".md", ""));
                }
            }
        } catch (IOException e) {
            logger.error("Error scanning internal plugins", e);
        }

        try {
            Path externalDir = Paths.get(externalConfigPath, "prompts");
            if (Files.exists(externalDir)) {
                Files.list(externalDir)
                        .filter(path -> path.toString().endsWith(".md"))
                        .forEach(path -> {
                            String filename = path.getFileName().toString();
                            plugins.add(filename.replace(".md", ""));
                        });
            }
        } catch (IOException e) {
            logger.error("Error scanning external plugins", e);
        }

        return new ArrayList<>(plugins);
    }

    @Override
    public boolean isPluginAvailable(String name) {
        return getAvailablePlugins().contains(name);
    }

    public Map<String, Object> getCacheStatistics() {
        var stats = pluginCache.stats();
        return Map.of(
                "hitRate", stats.hitRate(),
                "missRate", stats.missRate(),
                "loadCount", stats.loadCount(),
                "evictionCount", stats.evictionCount(),
                "estimatedSize", pluginCache.estimatedSize()
        );
    }

    @PreDestroy
    public void cleanup() {
        if (directoryWatcher != null) {
            try {
                directoryWatcher.close();
                logger.info("Directory watcher closed");
            } catch (IOException e) {
                logger.error("Error closing directory watcher", e);
            }
        }
        pluginCache.invalidateAll();
        logger.info("Plugin manager cleanup completed");
    }

    public enum PluginLoadingStrategy {
        EXTERNAL_FIRST,
        INTERNAL_FIRST,
        EXTERNAL_ONLY
    }
}
