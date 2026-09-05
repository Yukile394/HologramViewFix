package com.yukile.hologramviewfix.hologram;

import com.yukile.hologramviewfix.cache.HologramCache;
import com.yukile.hologramviewfix.util.ConfigManager;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Bridges raw entity scans into the {@link HologramCache}, using the
 * {@link HologramDetector} to classify entities as they're discovered
 * (chunk load, player join, etc.) and pruning entries whose entity no
 * longer exists.
 */
public final class HologramTracker {

    private final HologramCache cache;
    private final HologramDetector detector;
    private final ConfigManager configManager;
    private final LoggerUtil logger;

    public HologramTracker(HologramCache cache, HologramDetector detector,
                            ConfigManager configManager, LoggerUtil logger) {
        this.cache = cache;
        this.detector = detector;
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * Scans a single chunk's entities and registers any newly-detected
     * holograms. Safe to call repeatedly — already-tracked entities are
     * skipped cheaply.
     */
    public void scanChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        for (Entity entity : chunk.getEntities()) {
            scanEntity(entity);
        }
    }

    /**
     * Scans a single entity and registers it if it's a newly-detected
     * hologram. Returns the classified HologramInfo, or null if the entity
     * isn't a hologram (or is already tracked).
     */
    public HologramInfo scanEntity(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return null;
        }
        if (cache.isTracked(entity.getUniqueId())) {
            return cache.getByEntityId(entity.getUniqueId());
        }

        HologramInfo.SourceType type = detector.detect(entity);
        if (type == null) {
            return null;
        }

        HologramInfo info = new HologramInfo(entity.getUniqueId(), type, entity.getLocation());
        cache.add(info);
        logger.debug("Detected " + type + " hologram at " + shortLoc(entity));
        return info;
    }

    /**
     * Removes stale entries (entity no longer valid/exists) across an
     * entire world. Intended to run occasionally, not every tick.
     */
    public void pruneWorld(World world) {
        for (HologramInfo info : cache.getAll()) {
            if (info.getLocation() == null || !world.equals(info.getLocation().getWorld())) {
                continue;
            }
            Entity resolved = info.resolveEntity();
            if (resolved == null || !resolved.isValid()) {
                cache.remove(info);
            }
        }
    }

    public void untrack(Entity entity) {
        HologramInfo info = cache.getByEntityId(entity.getUniqueId());
        if (info != null) {
            cache.remove(info);
        }
    }

    public HologramCache getCache() {
        return cache;
    }

    private String shortLoc(Entity entity) {
        return entity.getWorld().getName() + " " + entity.getLocation().getBlockX()
                + "," + entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ();
    }
}
