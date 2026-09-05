package com.yukile.hologramviewfix.integration;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Common contract every third-party hologram plugin integration implements.
 * Each integration is responsible for:
 *  - detecting whether a given entity belongs to its hologram system
 *  - re-displaying that hologram to a specific viewer when required
 *
 * Integrations must never touch entities that don't belong to their own
 * system, and must never fail loudly if the target plugin is absent —
 * {@link #isAvailable()} guards every call site.
 */
public interface HologramIntegration {

    /**
     * @return true if the backing plugin is installed and hooked.
     */
    boolean isAvailable();

    /**
     * @return a short display name for status output (e.g. "Citizens").
     */
    String getName();

    /**
     * @return true if the given entity is recognized as belonging to this
     * hologram system.
     */
    boolean isHologram(Entity entity);

    /**
     * Re-sends / refreshes the hologram's visibility for the given viewer.
     * Implementations should use the underlying plugin's API where possible
     * rather than raw packet manipulation.
     */
    void refresh(HologramInfo info, Player viewer);
}
