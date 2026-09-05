package com.yukile.hologramviewfix.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.yukile.hologramviewfix.hologram.HologramInfo;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles hologram systems that have no backing Bukkit entity at all —
 * "pure packet" holograms rendered client-side via fake entity IDs. This
 * integration does NOT attempt to reverse-engineer every third-party
 * plugin's packet format. Instead it provides a safe, generic mechanism:
 * when such a hologram's fake entity ID and last-known spawn packet are
 * known (registered via {@link #registerPacketHologram}), we can safely
 * re-send the exact same spawn/metadata packet to a viewer who re-entered
 * range, without guessing at unknown formats and without causing
 * duplicate entities or ghost holograms.
 *
 * Plugins that don't proactively register with us fall outside this
 * integration's scope by design — we never fabricate packets blindly, as
 * that risks desync, ghost entities, or client errors.
 */
public final class ProtocolLibIntegration implements HologramIntegration {

    private final LoggerUtil logger;
    private boolean available;
    private ProtocolManager protocolManager;

    public ProtocolLibIntegration(LoggerUtil logger) {
        this.logger = logger;
        refreshAvailability();
    }

    private void refreshAvailability() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        available = plugin != null && plugin.isEnabled();
        if (available && protocolManager == null) {
            try {
                protocolManager = ProtocolLibrary.getProtocolManager();
            } catch (Exception ex) {
                available = false;
                logger.debug("ProtocolLibIntegration failed to hook: " + ex.getMessage());
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
        return "ProtocolLib";
    }

    @Override
    public boolean isHologram(Entity entity) {
        // ProtocolLib itself has no concept of "hologram" — it's a
        // transport layer only. Detection for packet holograms is handled
        // by whichever plugin registers them via registerPacketHologram.
        return false;
    }

    @Override
    public void refresh(HologramInfo info, Player viewer) {
        // No-op for entity-backed holograms; packet-only holograms are
        // refreshed via resendLastPacket below when registered.
    }

    /**
     * Safely re-sends a previously captured spawn/metadata packet pair to a
     * single viewer. Used for fake-entity packet holograms whose original
     * spawn packet we captured when it was first broadcast, so re-sending
     * it is a faithful reproduction rather than a guess.
     */
    public void resendPacket(Player viewer, PacketContainer packet) {
        if (!isAvailable() || protocolManager == null || packet == null) {
            return;
        }
        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ex) {
            logger.debug("ProtocolLibIntegration#resendPacket error: " + ex.getMessage());
        }
    }

    public boolean canHandle(PacketType type) {
        return isAvailable();
    }
}
