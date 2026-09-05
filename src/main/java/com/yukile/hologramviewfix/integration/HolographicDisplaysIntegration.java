package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integration for HolographicDisplays (both the legacy ArmorStand-based
 * versions and the newer Display-entity-based versions). Detection uses
 * class-name and metadata heuristics to remain compatible across the
 * plugin's major version changes without a compile-time dependency.
 */
public final class HolographicDisplaysIntegration implements HologramIntegration {

    private boolean available;
    private final LoggerUtil logger;

    public HolographicDisplaysIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HolographicDisplays");
        available = plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isAvailable() {
        refreshAvailability();
        return available;
    }

    @Override
    public String getName() {
        return "HolographicDisplays";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }
        try {
            String className = entity.getClass().getName().toLowerCase();
            if (className.contains("holographicdisplays")) {
                return true;
            }
            return entity.hasMetadata("HolographicDisplaysEntity");
        } catch (Exception ex) {
            logger.debug("HolographicDisplaysIntegration#isHologram error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void refresh(HologramInfo info, Player viewer) {
        if (!isAvailable()) {
            return;
        }
        try {
            Entity entity = info.resolveEntity();
            if (entity != null && viewer.isOnline()) {
                viewer.showEntity(org.bukkit.plugin.java.JavaPlugin.getPlugin(
                        com.yukile.hologramviewfix.HologramViewFix.class), entity);
            }
        } catch (Exception ex) {
            logger.debug("HolographicDisplaysIntegration#refresh error: " + ex.getMessage());
        }
    }
}
