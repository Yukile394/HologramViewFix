package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Integration for Citizens NPCs. Implemented via reflection against
 * Citizens' public API classes rather than a compile-time dependency,
 * since Citizens' snapshot Maven repository structure changes frequently
 * and is not reliable for CI builds. All reflective calls are wrapped
 * defensively — any failure simply results in this integration reporting
 * itself unavailable, never a plugin crash.
 *
 * We never touch NPC location, rotation, skin, name, traits, navigator,
 * pathfinding, equipment or metadata — we only ensure the NPC entity
 * (and any attached hologram, handled elsewhere) stays visible to nearby
 * players.
 */
public final class CitizensIntegration implements HologramIntegration {

    private final LoggerUtil logger;
    private boolean available;

    private Class<?> citizensApiClass;
    private Class<?> npcRegistryClass;
    private Class<?> npcClass;

    public CitizensIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
        available = citizens != null && citizens.isEnabled();
        if (available && citizensApiClass == null) {
            try {
                citizensApiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
                npcRegistryClass = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
                npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
                logger.debug("Citizens integration hooked successfully via reflection.");
            } catch (Throwable t) {
                available = false;
                logger.debug("Citizens API classes not found: " + t.getMessage());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        refreshAvailability();
        return available;
    }

    @Override
    public String getName() {
        return "Citizens";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }
        try {
            Object npc = resolveNpc(entity);
            return npc != null;
        } catch (Throwable t) {
            logger.debug("CitizensIntegration#isHologram error: " + t.getMessage());
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
            if (entity == null || viewer == null || !viewer.isOnline()) {
                return;
            }
            Object npc = resolveNpc(entity);
            if (npc == null) {
                return;
            }
            Method isSpawned = npcClass.getMethod("isSpawned");
            Object spawned = isSpawned.invoke(npc);
            if (!(spawned instanceof Boolean) || !((Boolean) spawned)) {
                return;
            }
            // Non-destructive visibility nudge: show the existing entity
            // packet-wise to the viewer without touching NPC state.
            viewer.showEntity(org.bukkit.plugin.java.JavaPlugin.getPlugin(
                    com.yukile.hologramviewfix.HologramViewFix.class), entity);
        } catch (Throwable t) {
            logger.debug("CitizensIntegration#refresh error: " + t.getMessage());
        }
    }

    private Object resolveNpc(Entity entity) throws Exception {
        Method getNpcRegistry = citizensApiClass.getMethod("getNPCRegistry");
        Object registry = getNpcRegistry.invoke(null);
        if (registry == null) {
            return null;
        }
        Method getNpc = npcRegistryClass.getMethod("getNPC", Entity.class);
        return getNpc.invoke(registry, entity);
    }
}
