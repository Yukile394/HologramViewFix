package com.yukile.hologramviewfix.listener;

import com.yukile.hologramviewfix.hologram.HologramTracker;
import com.yukile.hologramviewfix.util.ConfigManager;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Keeps the hologram cache in sync with chunk load/unload so holograms in
 * newly-loaded chunks are detected promptly, and so unloaded chunks don't
 * leave stale entries lingering in the cache indefinitely.
 */
public final class ChunkListener implements Listener {

    private final HologramTracker tracker;
    private final ConfigManager configManager;
    private final LoggerUtil logger;

    public ChunkListener(HologramTracker tracker, ConfigManager configManager, LoggerUtil logger) {
        this.tracker = tracker;
        this.configManager = configManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!configManager.isEnabled()) return;
        tracker.scanChunk(event.getChunk());
        logger.debug("Scanned chunk " + event.getChunk().getX() + "," + event.getChunk().getZ()
                + " in " + event.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // We intentionally do NOT immediately purge holograms in this chunk:
        // the entity objects become invalid naturally, and the periodic
        // tracker cycle prunes stale cache entries in batches. Aggressively
        // removing on every unload could cause needless re-detection churn
        // if the chunk reloads moments later (e.g. players near a border).
    }
}
