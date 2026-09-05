package com.yukile.hologramviewfix.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central accessor for config.yml values. Reloadable without a server
 * restart via {@link #reload()}.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;

    private boolean enabled;
    private boolean viewDistanceEnabled;
    private int viewDistance;
    private int minimumDistance;
    private int maximumDistance;
    private int checkInterval;
    private boolean chunkCheck;
    private boolean fixFlicker;
    private boolean fixMissingHolograms;
    private boolean fixDisplayEntities;
    private boolean fixArmorstandHolograms;
    private boolean fixCitizens;
    private boolean fixPacketHolograms;

    private boolean integrationCitizens;
    private boolean integrationDecentHolograms;
    private boolean integrationFancyHolograms;
    private boolean integrationCmi;
    private boolean integrationHolographicDisplays;
    private boolean integrationProtocolLib;

    private int maxChecksPerCycle;
    private boolean debug;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();

        enabled = cfg.getBoolean("enabled", true);

        viewDistanceEnabled = cfg.getBoolean("view-distance.enabled", true);
        viewDistance = cfg.getInt("view-distance.distance", 128);

        minimumDistance = cfg.getInt("minimum-distance", 0);
        maximumDistance = cfg.getInt("maximum-distance", 128);

        checkInterval = Math.max(1, cfg.getInt("check-interval", 5));
        chunkCheck = cfg.getBoolean("chunk-check", true);

        fixFlicker = cfg.getBoolean("fix-flicker", true);
        fixMissingHolograms = cfg.getBoolean("fix-missing-holograms", true);
        fixDisplayEntities = cfg.getBoolean("fix-display-entities", true);
        fixArmorstandHolograms = cfg.getBoolean("fix-armorstand-holograms", true);
        fixCitizens = cfg.getBoolean("fix-citizens", true);
        fixPacketHolograms = cfg.getBoolean("fix-packet-holograms", true);

        integrationCitizens = cfg.getBoolean("integrations.citizens", true);
        integrationDecentHolograms = cfg.getBoolean("integrations.decent-holograms", true);
        integrationFancyHolograms = cfg.getBoolean("integrations.fancy-holograms", true);
        integrationCmi = cfg.getBoolean("integrations.cmi", true);
        integrationHolographicDisplays = cfg.getBoolean("integrations.holographic-displays", true);
        integrationProtocolLib = cfg.getBoolean("integrations.protocol-lib", true);

        maxChecksPerCycle = Math.max(1, cfg.getInt("performance.max-checks-per-cycle", 100));
        debug = cfg.getBoolean("debug", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isViewDistanceEnabled() {
        return viewDistanceEnabled;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getMinimumDistance() {
        return minimumDistance;
    }

    public int getMaximumDistance() {
        return maximumDistance;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public boolean isChunkCheck() {
        return chunkCheck;
    }

    public boolean isFixFlicker() {
        return fixFlicker;
    }

    public boolean isFixMissingHolograms() {
        return fixMissingHolograms;
    }

    public boolean isFixDisplayEntities() {
        return fixDisplayEntities;
    }

    public boolean isFixArmorstandHolograms() {
        return fixArmorstandHolograms;
    }

    public boolean isFixCitizens() {
        return fixCitizens;
    }

    public boolean isFixPacketHolograms() {
        return fixPacketHolograms;
    }

    public boolean isIntegrationCitizens() {
        return integrationCitizens;
    }

    public boolean isIntegrationDecentHolograms() {
        return integrationDecentHolograms;
    }

    public boolean isIntegrationFancyHolograms() {
        return integrationFancyHolograms;
    }

    public boolean isIntegrationCmi() {
        return integrationCmi;
    }

    public boolean isIntegrationHolographicDisplays() {
        return integrationHolographicDisplays;
    }

    public boolean isIntegrationProtocolLib() {
        return integrationProtocolLib;
    }

    public int getMaxChecksPerCycle() {
        return maxChecksPerCycle;
    }

    public boolean isDebug() {
        return debug;
    }
}
