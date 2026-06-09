package com.javamsdt.agent.config.plugin;

import java.util.List;
import java.util.Map;

public interface PluginManager<T> {

    T loadPlugin(String name);

    T loadPlugin(String name, Map<String, Object> context);

    void reloadPlugin(String name);

    void reloadAllPlugins();

    List<String> getAvailablePlugins();

    boolean isPluginAvailable(String name);
}
