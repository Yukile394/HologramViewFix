package com.yukile.hologramviewfix.hologram;

import com.yukile.hologramviewfix.cache.HologramCache;
import com.yukile.hologramviewfix.integration.HologramIntegration;
import com.yukile.hologramviewfix.util.ConfigManager;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides, per (player, hologram) pair, whether a visibility refresh is
 * needed, and performs it through the appropriate integration. Maintains a
 * visibility-state cache to prevent flicker: we never re-issue a
 * visible/hidden transition if the last known state already matches.
 */
public final class HologramVisibilityManager {

    /** Key: playerId + ":" + entityId -> last known state. */
    private final Map<String, HologramInfo.VisibilityState> stateCache = new ConcurrentHashMap<>();

    private final HologramCache cache;
    private final List<HologramIntegration> integrations;
    private final ConfigManager configManager;
    private final LoggerUtil logger;

    public HologramVisibilityManager(HologramCache cache, List<HologramIntegration> integrations,
                                      ConfigManager configManager, LoggerUtil logger) {
        this.cache = cache;
        this.integrations = integrations;
        this.configManager = configManager;
        this.logger = logger;
    }

    private String key(UUID player, UUID entity) {
        return player + ":" + entity;
    }

    /**
     * Ensures the given hologram is visible to the given player if it's
     * within the configured view distance, correcting client-side tracking
     * gaps without spawning duplicates or spamming packets.
     */
    public void ensureVisible(Player player, HologramInfo info) {
        if (player == null || !player.isOnline() || info == null || !info.isValid()) {
            return;
        }
        if (info.getLocation() == null || info.getLocation().getWorld() == null) {
            return;
        }
        if (!player.getWorld().equals(info.getLocation().getWorld())) {
            return;
        }

        double distance = player.getLocation().distance(info.getLocation());
        int viewDistance = configManager.isViewDistanceEnabled()
                ? configManager.getViewDistance()
                : configManager.getMaximumDistance();

        String cacheKey = key(player.getUniqueId(), info.getEntityId());
        HologramInfo.VisibilityState lastState = stateCache.getOrDefault(cacheKey, HologramInfo.VisibilityState.UNKNOWN);

        if (distance > viewDistance) {
            // Out of configured range — nothing to do; let natural tracking
            // handle hide, we don't force-hide holograms ourselves.
            return;
        }

        if (lastState == HologramInfo.VisibilityState.VISIBLE) {
            // Flicker prevention: already known visible, skip redundant work.
            return;
        }

        Entity entity = info.resolveEntity();
        if (entity == null) {
            // Entity no longer exists in a loaded chunk — nothing to refresh.
            return;
        }

        HologramIntegration owningIntegration = findIntegrationFor(info.getSourceType());
        try {
            if (owningIntegration != null && owningIntegration.isAvailable()) {
                owningIntegration.refresh(info, player);
            } else {
                // Generic fallback for ArmorStand/Display holograms with no
                // dedicated plugin integration: a plain re-show is safe
                // and non-destructive, and Paper de-duplicates automatically
                // if the entity is already shown.
                player.showEntity(org.bukkit.plugin.java.JavaPlugin.getPlugin(
                        com.yukile.hologramviewfix.HologramViewFix.class), entity);
            }
            stateCache.put(cacheKey, HologramInfo.VisibilityState.VISIBLE);
            logger.debug("Visibility restored for " + info.getSourceType() + " to " + player.getName());
        } catch (Exception ex) {
            logger.debug("Failed to refresh visibility: " + ex.getMessage());
        }
    }

    /**
     * Marks a hologram as hidden for a player without issuing any packets —
     * used purely to keep the flicker-prevention cache in sync when we
     * detect the player left range naturally.
     */
    public void markHidden(Player player, HologramInfo info) {
        stateCache.put(key(player.getUniqueId(), info.getEntityId()), HologramInfo.VisibilityState.HIDDEN);
    }

    public void clearForPlayer(UUID playerId) {
        stateCache.keySet().removeIf(k -> k.startsWith(playerId + ":"));
    }

    public void clearAll() {
        stateCache.clear();
    }

    private HologramIntegration findIntegrationFor(HologramInfo.SourceType sourceType) {
        String targetName = switch (sourceType) {
            case CITIZENS_NPC -> "Citizens";
            case DECENT_HOLOGRAMS -> "DecentHolograms";
            case FANCY_HOLOGRAMS -> "FancyHolograms";
            case CMI -> "CMI";
            case HOLOGRAPHIC_DISPLAYS -> "HolographicDisplays";
            default -> null;
        };
        if (targetName == null) {
            return null;
        }
        for (HologramIntegration integration : integrations) {
            if (integration.getName().equals(targetName)) {
                return integration;
            }
        }
        return null;
    }
}
