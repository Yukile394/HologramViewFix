package com.yukile.hologramviewfix.hologram;

import com.yukile.hologramviewfix.integration.HologramIntegration;
import com.yukile.hologramviewfix.util.LoggerUtil;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;

/**
 * Multi-layer hologram detection. Order of precedence, from most to least
 * authoritative:
 *
 *  1. Third-party hologram plugin API / integration match
 *  2. Plugin-specific identifier (namespaced PDC keys)
 *  3. Generic PersistentDataContainer markers
 *  4. Entity metadata (Bukkit legacy metadata API)
 *  5. Citizens API (handled inside the Citizens integration itself)
 *  6. Display entity characteristics
 *  7. ArmorStand characteristics
 *  8. Combination heuristic: custom name + invisible + marker + no-gravity
 *  9. Safe fallback: if nothing matches confidently, it is NOT a hologram
 *
 * A bare ArmorStand alone is never enough — we require a combination of
 * signals to minimize false positives against normal decorative or
 * gameplay entities.
 */
public final class HologramDetector {

    private final List<HologramIntegration> integrations;
    private final LoggerUtil logger;

    public HologramDetector(List<HologramIntegration> integrations, LoggerUtil logger) {
        this.integrations = integrations;
        this.logger = logger;
    }

    /**
     * Attempts to classify the given entity. Returns null if the entity is
     * not confidently recognized as a hologram.
     */
    public HologramInfo.SourceType detect(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return null;
        }

        // Layer 1: hologram plugin API / registered integrations.
        for (HologramIntegration integration : integrations) {
            try {
                if (integration.isAvailable() && integration.isHologram(entity)) {
                    return mapIntegrationToSource(integration.getName());
                }
            } catch (Exception ex) {
                logger.debug("Integration " + integration.getName() + " threw during detection: " + ex.getMessage());
            }
        }

        // Layer 2 & 3: PersistentDataContainer markers (generic, plugin-agnostic).
        if (hasHologramLikePdcKey(entity)) {
            return classifyByEntityShape(entity);
        }

        // Layer 4: legacy Bukkit metadata markers.
        if (hasHologramLikeMetadata(entity)) {
            return classifyByEntityShape(entity);
        }

        // Layer 6: Display entity characteristics.
        if (entity instanceof TextDisplay) {
            return HologramInfo.SourceType.TEXT_DISPLAY;
        }
        if (entity instanceof ItemDisplay && looksLikeHologramDisplay((Display) entity)) {
            return HologramInfo.SourceType.ITEM_DISPLAY;
        }
        if (entity instanceof BlockDisplay && looksLikeHologramDisplay((Display) entity)) {
            return HologramInfo.SourceType.BLOCK_DISPLAY;
        }

        // Layer 7 & 8: ArmorStand combination heuristic.
        if (entity instanceof ArmorStand armorStand) {
            if (isHologramArmorStand(armorStand)) {
                return HologramInfo.SourceType.ARMORSTAND;
            }
        }

        // Layer 9: safe fallback — not a hologram.
        return null;
    }

    private HologramInfo.SourceType mapIntegrationToSource(String integrationName) {
        return switch (integrationName) {
            case "Citizens" -> HologramInfo.SourceType.CITIZENS_NPC;
            case "DecentHolograms" -> HologramInfo.SourceType.DECENT_HOLOGRAMS;
            case "FancyHolograms" -> HologramInfo.SourceType.FANCY_HOLOGRAMS;
            case "CMI" -> HologramInfo.SourceType.CMI;
            case "HolographicDisplays" -> HologramInfo.SourceType.HOLOGRAPHIC_DISPLAYS;
            default -> HologramInfo.SourceType.UNKNOWN;
        };
    }

    private HologramInfo.SourceType classifyByEntityShape(Entity entity) {
        if (entity instanceof TextDisplay) return HologramInfo.SourceType.TEXT_DISPLAY;
        if (entity instanceof ItemDisplay) return HologramInfo.SourceType.ITEM_DISPLAY;
        if (entity instanceof BlockDisplay) return HologramInfo.SourceType.BLOCK_DISPLAY;
        if (entity instanceof ArmorStand) return HologramInfo.SourceType.ARMORSTAND;
        return HologramInfo.SourceType.UNKNOWN;
    }

    private boolean hasHologramLikePdcKey(Entity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            for (var key : pdc.getKeys()) {
                String ns = key.getNamespace().toLowerCase();
                String k = key.getKey().toLowerCase();
                if (ns.contains("hologram") || k.contains("hologram")
                        || ns.contains("holo") || k.contains("holo")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Defensive: never let a malformed PDC crash detection.
        }
        return false;
    }

    private boolean hasHologramLikeMetadata(Entity entity) {
        try {
            for (String key : new String[]{"hologram", "Hologram", "HOLOGRAM", "isHologram"}) {
                if (entity.hasMetadata(key)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Defensive.
        }
        return false;
    }

    /**
     * A Display entity is only treated as a hologram candidate if it shows
     * hologram-like traits (no collision interaction expected + used purely
     * for rendering). We deliberately do NOT flag every ItemDisplay/
     * BlockDisplay in the world — only ones already caught by the stronger
     * signals above, or those with zero transformation scale oddities that
     * are extremely unlikely for legitimate decoration.
     * By default, without a plugin marker, we treat unmarked Item/Block
     * displays conservatively and do NOT claim them as holograms, since
     * they's used extensively for legitimate build decoration.
     */
    private boolean looksLikeHologramDisplay(Display display) {
        // Conservative by design: without a plugin marker (already checked
        // above), unmarked Item/Block displays are left alone entirely to
        // avoid false positives against map art, builds, and decorations.
        return false;
    }

    /**
     * The classic hologram ArmorStand signature: invisible + marker +
     * no-gravity + has a custom name that is set visible. Any single trait
     * alone (e.g. just "invisible") is extremely common on legitimate
     * decorative or plugin-managed ArmorStands, so we require the full
     * combination before classifying it as a hologram.
     */
    private boolean isHologramArmorStand(ArmorStand armorStand) {
        boolean invisible = armorStand.isInvisible();
        boolean marker = armorStand.isMarker();
        boolean noGravity = !armorStand.hasGravity();
        boolean hasCustomName = armorStand.getCustomName() != null && !armorStand.getCustomName().isEmpty();
        boolean nameVisible = armorStand.isCustomNameVisible();

        int signals = 0;
        if (invisible) signals++;
        if (marker) signals++;
        if (noGravity) signals++;
        if (hasCustomName && nameVisible) signals++;

        // Require at least 3 of the 4 classic signals, and custom name
        // must always be present — a hologram without visible text is
        // essentially never the intent.
        return hasCustomName && nameVisible && signals >= 3;
    }
}
