package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Integration for FancyHolograms. FancyHolograms builds its holograms on top
 * of real Display entities (TextDisplay/ItemDisplay/BlockDisplay) tagged
 * with its own persistent data keys, so detection can largely rely on PDC
 * inspection rather than deep reflection.
 */
public final class FancyHologramsIntegration implements HologramIntegration {

    private boolean available;
    private final LoggerUtil logger;

    public FancyHologramsIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        available = plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isAvailable() {
        refreshAvailability();
        return available;
    }

    @Override
    public String getName() {
        return "FancyHolograms";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable() || !(entity instanceof Display)) {
            return false;
        }
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            for (var key : pdc.getKeys()) {
                if (key.getNamespace().equalsIgnoreCase("fancyholograms")) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            logger.debug("FancyHologramsIntegration#isHologram error: " + ex.getMessage());
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
            logger.debug("FancyHologramsIntegration#refresh error: " + ex.getMessage());
        }
    }
}
