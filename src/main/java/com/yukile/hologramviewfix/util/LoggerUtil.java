package com.yukile.hologramviewfix.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Debug logger with repeat-suppression so the same recurring message can't
 * spam the console when debug mode is enabled.
 */
public final class LoggerUtil {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<String, Long> lastLogged = new ConcurrentHashMap<>();

    private static final long SUPPRESS_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(10);

    public LoggerUtil(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void debug(String message) {
        if (!configManager.isDebug()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastLogged.get(message);
        if (last != null && (now - last) < SUPPRESS_WINDOW_MILLIS) {
            return;
        }
        lastLogged.put(message, now);
        plugin.getLogger().info("[Debug] " + message);
    }

    public void info(String message) {
        plugin.getLogger().info(message);
    }

    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    public void error(String message, Throwable throwable) {
        plugin.getLogger().severe(message);
        if (throwable != null && configManager.isDebug()) {
            throwable.printStackTrace();
        }
    }
}
