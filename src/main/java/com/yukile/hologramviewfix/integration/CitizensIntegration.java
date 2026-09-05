package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integration for Citizens NPCs. We never touch NPC location, rotation,
 * skin, name, traits, navigator, pathfinding, equipment or metadata — we
 * only ensure the NPC entity (and any attached hologram, handled elsewhere)
 * stays visible to nearby players.
 */
public final class CitizensIntegration implements HologramIntegration {

    private final LoggerUtil logger;
    private boolean available;

    public CitizensIntegration(LoggerUtil logger) {
        this.logger = logger;
        Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
        this.available = citizens != null && citizens.isEnabled();
        if (available) {
            logger.debug("Citizens integration hooked successfully.");
        }
    }

    @Override
    public boolean isAvailable() {
        Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
        available = citizens != null && citizens.isEnabled();
        return available;
    }

    @Override
    public String getName() {
        return "Citizens";
    }

    @Override
    public boolean isHologram(Entity entity) {
        if (!isAvailable()) {
            return false;
        }
        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            if (registry == null) {
                return false;
            }
            NPC npc = registry.getNPC(entity);
            return npc != null;
        } catch (Exception ex) {
            logger.debug("CitizensIntegration#isHologram error: " + ex.getMessage());
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
            if (entity == null) {
                return;
            }
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            NPC npc = registry.getNPC(entity);
            if (npc == null || !npc.isSpawned()) {
                return;
            }
            // Non-destructive visibility nudge: show the existing entity
            // packet-wise to the viewer without touching NPC state.
            viewer.showEntity(org.bukkit.plugin.java.JavaPlugin.getPlugin(
                    com.yukile.hologramviewfix.HologramViewFix.class), entity);
        } catch (Exception ex) {
            logger.debug("CitizensIntegration#refresh error: " + ex.getMessage());
        }
    }
}
