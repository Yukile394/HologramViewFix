package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integration for CMI holograms. CMI does not expose a stable public API
 * artifact, so detection relies on entity metadata/class-name heuristics
 * combined with a safe fallback resend rather than deep reflection into
 * CMI internals (which change frequently between CMI versions).
 */
public final class CMIIntegration implements HologramIntegration {

    private boolean available;
    private final LoggerUtil logger;

    public CMIIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        available = plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isAvailable() {
        refreshAvailability();
        return available;
    }

    @Override
    public String getName() {
        return "CMI";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }
        try {
            if (!entity.hasMetadata("CMI_Hologram") && !entity.hasMetadata("cmi-hologram")) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            logger.debug("CMIIntegration#isHologram error: " + ex.getMessage());
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
            logger.debug("CMIIntegration#refresh error: " + ex.getMessage());
        }
    }
}
