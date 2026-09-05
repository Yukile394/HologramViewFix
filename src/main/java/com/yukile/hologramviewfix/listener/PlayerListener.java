package com.yukile.hologramviewfix.listener;

import com.yukile.hologramviewfix.cache.HologramCache;
import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.hologram.HologramTracker;
import com.yukile.hologramviewfix.hologram.HologramVisibilityManager;
import com.yukile.hologramviewfix.util.ConfigManager;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;

/**
 * Reacts to player lifecycle/movement events that can cause client-side
 * hologram tracking gaps: join, world change, teleport, respawn, and
 * chunk-boundary crossing during normal movement.
 *
 * PlayerMoveEvent is intentionally filtered to chunk-boundary crossings
 * only — we never re-scan on every single move tick, both for performance
 * and to avoid packet spam.
 */
public final class PlayerListener implements Listener {

    private final HologramTracker tracker;
    private final HologramVisibilityManager visibilityManager;
    private final ConfigManager configManager;
    private final LoggerUtil logger;

    public PlayerListener(HologramTracker tracker, HologramVisibilityManager visibilityManager,
                           ConfigManager configManager, LoggerUtil logger) {
        this.tracker = tracker;
        this.visibilityManager = visibilityManager;
        this.configManager = configManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!configManager.isEnabled()) return;
        refreshAround(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        visibilityManager.clearForPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!configManager.isEnabled()) return;
        visibilityManager.clearForPlayer(event.getPlayer().getUniqueId());
        refreshAround(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!configManager.isEnabled()) return;
        // Small delay isn't needed here; the tracker cycle also sweeps this,
        // but an immediate refresh avoids the "pop-in after 1 second" feel.
        refreshAround(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!configManager.isEnabled()) return;
        refreshAround(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (!configManager.isEnabled() || !configManager.isChunkCheck()) return;
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
            // Same chunk — not a meaningful boundary crossing, skip entirely.
            return;
        }
        refreshAround(event.getPlayer());
    }

    private void refreshAround(org.bukkit.entity.Player player) {
        Chunk center = player.getLocation().getChunk();
        Collection<HologramInfo> nearby = tracker.getCache().getNear(
                player.getWorld(), center.getX(), center.getZ(), viewDistanceInChunks());

        int checked = 0;
        int maxChecks = configManager.getMaxChecksPerCycle();
        for (HologramInfo info : nearby) {
            if (checked++ >= maxChecks) {
                break;
            }
            visibilityManager.ensureVisible(player, info);
        }
    }

    private int viewDistanceInChunks() {
        int blocks = configManager.isViewDistanceEnabled()
                ? configManager.getViewDistance()
                : configManager.getMaximumDistance();
        return Math.max(1, (blocks / 16) + 1);
    }
}
