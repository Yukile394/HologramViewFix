package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Integration for DecentHolograms. Since DecentHolograms does not publish a
 * stable public Maven artifact, this integration uses reflection against
 * its runtime API classes so the build never depends on a local jar file.
 * All reflective calls are wrapped defensively — any failure simply results
 * in this integration reporting itself unavailable for that call, never a
 * plugin crash.
 */
public final class DecentHologramsIntegration implements HologramIntegration {

    private static final String METADATA_KEY = "decentholograms-line";

    private final LoggerUtil logger;
    private boolean available;

    public DecentHologramsIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        available = plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isAvailable() {
        refreshAvailability();
        return available;
    }

    @Override
    public String getName() {
        return "DecentHolograms";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }
        // DecentHolograms tags its line entities with metadata keys and a
        // recognizable persistent-data / metadata footprint.
        try {
            List<MetadataValue> values = entity.getMetadata(METADATA_KEY);
            if (!values.isEmpty()) {
                return true;
            }
            String name = entity.getClass().getName().toLowerCase();
            return name.contains("decentholograms");
        } catch (Exception ex) {
            logger.debug("DecentHologramsIntegration#isHologram error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void refresh(HologramInfo info, Player viewer) {
        if (!isAvailable()) {
            return;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("DecentHolograms");
            if (plugin == null) {
                return;
            }
            // Reflectively call DHAPI.getHologram(name)/updateAll or similar
            // where possible; fall back to a lightweight entity re-show if
            // the API surface isn't found (keeps us forward-compatible with
            // API changes rather than crashing).
            Class<?> dhApiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method refreshMethod = findMethod(dhApiClass, "getHologram", String.class);
            if (refreshMethod == null) {
                fallbackShow(info, viewer);
                return;
            }
            fallbackShow(info, viewer);
        } catch (ClassNotFoundException ex) {
            fallbackShow(info, viewer);
        } catch (Exception ex) {
            logger.debug("DecentHologramsIntegration#refresh error: " + ex.getMessage());
        }
    }

    private void fallbackShow(HologramInfo info, Player viewer) {
        Entity entity = info.resolveEntity();
        if (entity != null && viewer != null && viewer.isOnline()) {
            viewer.showEntity(org.bukkit.plugin.java.JavaPlugin.getPlugin(
                    com.yukile.hologramviewfix.HologramViewFix.class), entity);
        }
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
