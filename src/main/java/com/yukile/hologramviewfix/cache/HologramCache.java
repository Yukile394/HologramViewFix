package com.yukile.hologramviewfix.cache;

import com.yukile.hologramviewfix.hologram.HologramInfo;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fast lookup structure: World UUID -> Chunk key -> set of tracked holograms.
 * Lets the tracker cycle only inspect holograms near a moving/joining player
 * instead of scanning every entity on the server every tick.
 */
public final class HologramCache {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Set<HologramInfo>>> worldMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, HologramInfo> byEntityId = new ConcurrentHashMap<>();

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public void add(HologramInfo info) {
        if (info.getLocation() == null || info.getLocation().getWorld() == null) {
            return;
        }
        World world = info.getLocation().getWorld();
        int chunkX = info.getLocation().getBlockX() >> 4;
        int chunkZ = info.getLocation().getBlockZ() >> 4;

        worldMap
                .computeIfAbsent(world.getUID(), w -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey(chunkX, chunkZ), c -> ConcurrentHashMap.newKeySet())
                .add(info);

        byEntityId.put(info.getEntityId(), info);
    }

    public void remove(HologramInfo info) {
        if (info.getLocation() == null || info.getLocation().getWorld() == null) {
            byEntityId.remove(info.getEntityId());
            return;
        }
        World world = info.getLocation().getWorld();
        int chunkX = info.getLocation().getBlockX() >> 4;
        int chunkZ = info.getLocation().getBlockZ() >> 4;

        ConcurrentHashMap<Long, Set<HologramInfo>> chunks = worldMap.get(world.getUID());
        if (chunks != null) {
            Set<HologramInfo> set = chunks.get(chunkKey(chunkX, chunkZ));
            if (set != null) {
                set.remove(info);
                if (set.isEmpty()) {
                    chunks.remove(chunkKey(chunkX, chunkZ));
                }
            }
        }
        byEntityId.remove(info.getEntityId());
    }

    public HologramInfo getByEntityId(UUID entityId) {
        return byEntityId.get(entityId);
    }

    public boolean isTracked(UUID entityId) {
        return byEntityId.containsKey(entityId);
    }

    public Collection<HologramInfo> getInChunk(World world, int chunkX, int chunkZ) {
        ConcurrentHashMap<Long, Set<HologramInfo>> chunks = worldMap.get(world.getUID());
        if (chunks == null) {
            return Collections.emptySet();
        }
        Set<HologramInfo> set = chunks.get(chunkKey(chunkX, chunkZ));
        return set == null ? Collections.emptySet() : set;
    }

    /**
     * Returns all holograms within a radius (in chunks) of the given chunk
     * coordinates, in the given world.
     */
    public Collection<HologramInfo> getNear(World world, int centerChunkX, int centerChunkZ, int chunkRadius) {
        ConcurrentHashMap<Long, Set<HologramInfo>> chunks = worldMap.get(world.getUID());
        if (chunks == null) {
            return Collections.emptySet();
        }
        Set<HologramInfo> result = ConcurrentHashMap.newKeySet();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                Set<HologramInfo> set = chunks.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                if (set != null) {
                    result.addAll(set);
                }
            }
        }
        return result;
    }

    public Collection<HologramInfo> getAll() {
        return Collections.unmodifiableCollection(byEntityId.values());
    }

    public int size() {
        return byEntityId.size();
    }

    public void clear() {
        worldMap.clear();
        byEntityId.clear();
    }

    public void clearWorld(World world) {
        ConcurrentHashMap<Long, Set<HologramInfo>> chunks = worldMap.remove(world.getUID());
        if (chunks != null) {
            for (Set<HologramInfo> set : chunks.values()) {
                for (HologramInfo info : set) {
                    byEntityId.remove(info.getEntityId());
                }
            }
        }
    }

    /**
     * Helper for a chunk object directly.
     */
    public Collection<HologramInfo> getInChunk(Chunk chunk) {
        return getInChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
}
