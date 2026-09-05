package com.yukile.hologramviewfix;

import com.yukile.hologramviewfix.cache.HologramCache;
import com.yukile.hologramviewfix.command.HologramViewFixCommand;
import com.yukile.hologramviewfix.hologram.HologramDetector;
import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.hologram.HologramTracker;
import com.yukile.hologramviewfix.hologram.HologramVisibilityManager;
import com.yukile.hologramviewfix.integration.CMIIntegration;
import com.yukile.hologramviewfix.integration.CitizensIntegration;
import com.yukile.hologramviewfix.integration.DecentHologramsIntegration;
import com.yukile.hologramviewfix.integration.FancyHologramsIntegration;
import com.yukile.hologramviewfix.integration.HologramIntegration;
import com.yukile.hologramviewfix.integration.HolographicDisplaysIntegration;
import com.yukile.hologramviewfix.integration.ProtocolLibIntegration;
import com.yukile.hologramviewfix.listener.ChunkListener;
import com.yukile.hologramviewfix.listener.PlayerListener;
import com.yukile.hologramviewfix.util.ConfigManager;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * HologramViewFix — fixes hologram visibility, tracking, and flicker issues
 * across the most common Paper hologram systems (ArmorStand, Display
 * entities, Citizens, DecentHolograms, FancyHolograms, CMI,
 * HolographicDisplays, and packet-based systems via ProtocolLib).
 */
public final class HologramViewFix extends JavaPlugin {

    private ConfigManager configManager;
    private LoggerUtil loggerUtil;
    private HologramCache hologramCache;
    private HologramDetector hologramDetector;
    private HologramTracker hologramTracker;
    private HologramVisibilityManager visibilityManager;
    private List<HologramIntegration> integrations;

    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.loggerUtil = new LoggerUtil(this, configManager);

        setupIntegrations();

        this.hologramCache = new HologramCache();
        this.hologramDetector = new HologramDetector(integrations, loggerUtil);
        this.hologramTracker = new HologramTracker(hologramCache, hologramDetector, configManager, loggerUtil);
        this.visibilityManager = new HologramVisibilityManager(hologramCache, integrations, configManager, loggerUtil);

        registerListeners();
        registerCommand();
        performInitialScan();
        startTickTask();

        loggerUtil.info("HologramViewFix has been enabled. Tracking " + hologramCache.size() + " holograms so far.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (hologramCache != null) {
            hologramCache.clear();
        }
        if (visibilityManager != null) {
            visibilityManager.clearAll();
        }
        // We deliberately do NOT destroy any hologram entities, NPCs, or
        // third-party plugin data on disable — this plugin only manages
        // visibility, never entity lifecycle ownership.
        loggerUtil.info("HologramViewFix has been disabled cleanly.");
    }

    private void setupIntegrations() {
        integrations = new ArrayList<>();
        if (configManager.isIntegrationCitizens()) {
            integrations.add(new CitizensIntegration(loggerUtil));
        }
        if (configManager.isIntegrationDecentHolograms()) {
            integrations.add(new DecentHologramsIntegration(loggerUtil));
        }
        if (configManager.isIntegrationFancyHolograms()) {
            integrations.add(new FancyHologramsIntegration(loggerUtil));
        }
        if (configManager.isIntegrationCmi()) {
            integrations.add(new CMIIntegration(loggerUtil));
        }
        if (configManager.isIntegrationHolographicDisplays()) {
            integrations.add(new HolographicDisplaysIntegration(loggerUtil));
        }
        if (configManager.isIntegrationProtocolLib()) {
            integrations.add(new ProtocolLibIntegration(loggerUtil));
        }

        for (HologramIntegration integration : integrations) {
            if (integration.isAvailable()) {
                loggerUtil.info("Hooked into " + integration.getName() + ".");
            }
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
                new PlayerListener(hologramTracker, visibilityManager, configManager, loggerUtil), this);
        Bukkit.getPluginManager().registerEvents(
                new ChunkListener(hologramTracker, configManager, loggerUtil), this);
    }

    private void registerCommand() {
        var command = getCommand("hologramviewfix");
        if (command != null) {
            HologramViewFixCommand executor = new HologramViewFixCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void performInitialScan() {
        for (var world : Bukkit.getWorlds()) {
            for (var chunk : world.getLoadedChunks()) {
                hologramTracker.scanChunk(chunk);
            }
        }
    }

    /**
     * Periodic cycle: prunes stale cache entries and, batched by
     * max-checks-per-cycle, ensures nearby holograms remain visible to
     * online players. This is the safety-net sweep that catches anything
     * event-based triggers might have missed, without scanning every
     * entity on the server each run.
     */
    private void startTickTask() {
        long interval = configManager.getCheckInterval();
        tickTask = Bukkit.getScheduler().runTaskTimer(this, this::runTickCycle, interval, interval);
    }

    private void runTickCycle() {
        if (!configManager.isEnabled()) {
            return;
        }
        int maxChecks = configManager.getMaxChecksPerCycle();
        int checked = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (checked >= maxChecks) {
                break;
            }
            var center = player.getLocation().getChunk();
            int radius = Math.max(1, (configManager.getViewDistance() / 16) + 1);
            Collection<HologramInfo> nearby = hologramCache.getNear(
                    player.getWorld(), center.getX(), center.getZ(), radius);

            for (HologramInfo info : nearby) {
                if (checked++ >= maxChecks) {
                    break;
                }
                visibilityManager.ensureVisible(player, info);
            }
        }

        // Occasionally prune stale entries in all worlds with online players.
        for (var world : Bukkit.getWorlds()) {
            hologramTracker.pruneWorld(world);
        }
    }

    public void reloadPlugin() {
        configManager.reload();
        if (tickTask != null) {
            tickTask.cancel();
        }
        startTickTask();
        setupIntegrations();
        hologramDetector = new HologramDetector(integrations, loggerUtil);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LoggerUtil getLoggerUtil() {
        return loggerUtil;
    }

    public HologramCache getHologramCache() {
        return hologramCache;
    }

    public HologramTracker getHologramTracker() {
        return hologramTracker;
    }

    public HologramVisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    public List<HologramIntegration> getIntegrations() {
        return integrations;
    }
}
