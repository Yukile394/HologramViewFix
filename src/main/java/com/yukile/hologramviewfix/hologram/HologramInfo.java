package com.yukile.hologramviewfix.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Immutable-ish descriptor of a single tracked hologram, regardless of which
 * underlying system created it. This is the common representation every
 * detector and integration normalizes into.
 */
public final class HologramInfo {

    /**
     * Visibility state used to avoid flicker: we only re-send/hide packets
     * when the state actually changes for a given viewer.
     */
    public enum VisibilityState {
        VISIBLE,
        HIDDEN,
        UNKNOWN
    }

    /**
     * Where this hologram came from. Used purely for diagnostics/status
     * output and to pick the right integration when a refresh is needed.
     */
    public enum SourceType {
        ARMORSTAND,
        TEXT_DISPLAY,
        ITEM_DISPLAY,
        BLOCK_DISPLAY,
        CITIZENS_NPC,
        DECENT_HOLOGRAMS,
        FANCY_HOLOGRAMS,
        CMI,
        HOLOGRAPHIC_DISPLAYS,
        PACKET_GENERIC,
        UNKNOWN
    }

    private final UUID entityId;
    private final SourceType sourceType;
    private volatile Location location;
    private volatile boolean valid = true;

    public HologramInfo(UUID entityId, SourceType sourceType, Location location) {
        this.entityId = entityId;
        this.sourceType = sourceType;
        this.location = location;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public Location getLocation() {
        return location;
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }

    /**
     * Resolves the live Bukkit entity for this hologram, if it still exists
     * in a loaded chunk. Returns null for packet-only holograms (no backing
     * entity) or if the entity has since been removed.
     */
    public Entity resolveEntity() {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        for (Entity entity : location.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(entityId)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HologramInfo other)) return false;
        return entityId.equals(other.entityId);
    }

    @Override
    public int hashCode() {
        return entityId.hashCode();
    }
}
