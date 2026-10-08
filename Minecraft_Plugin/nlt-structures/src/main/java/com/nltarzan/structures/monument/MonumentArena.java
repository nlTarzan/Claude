package com.nltarzan.structures.monument;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * The area around the monument center. Replaces the datapack's marker entity, the hardcoded
 * {@code forceload} coordinates and the {@code distance=..40} selectors.
 */
public final class MonumentArena {

    /** How long to wait for the area's entities to load before giving up on a check. */
    private static final int ENTITY_LOAD_TIMEOUT_TICKS = 200;

    /** The monument box, in blocks from the center: its four 29-wide quadrants and 22 levels. */
    private static final int BOX_WEST = -29;
    private static final int BOX_EAST = 28;
    private static final int BOX_BOTTOM = -1;
    private static final int BOX_TOP = 20;

    private final Plugin plugin;
    private final World world;
    private final Location center;
    private final double radius;
    private final List<ChunkPos> chunks = new ArrayList<>();

    private record ChunkPos(int x, int z) {
    }

    public MonumentArena(Plugin plugin, World world, int x, int y, int z, double radius) {
        this.plugin = plugin;
        this.world = world;
        this.center = new Location(world, x, y, z);
        this.radius = radius;

        // The chunks covering the radius and the whole monument box, whichever reaches further.
        int r = (int) Math.ceil(radius);
        int west = Math.min(x - r, x + BOX_WEST);
        int east = Math.max(x + r, x + BOX_EAST);
        int north = Math.min(z - r, z + BOX_WEST);
        int south = Math.max(z + r, z + BOX_EAST);
        for (int cx = west >> 4; cx <= east >> 4; cx++) {
            for (int cz = north >> 4; cz <= south >> 4; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
    }

    public World world() {
        return world;
    }

    /** The block corner the four quadrants are placed around. Returns a copy. */
    public Location center() {
        return center.clone();
    }

    /**
     * Whether a location counts as "at the monument": within check-radius of the center, or anywhere
     * in the monument box. The box matters for its far corners, which are more than 40 blocks away.
     * Used for counting Elder Guardians and players.
     */
    public boolean contains(Location location) {
        return location.getWorld() == world && isAround(location.getX() - center.getX(),
                location.getY() - center.getY(), location.getZ() - center.getZ(), radius);
    }

    /** {@link #contains}, by the offset from the center. */
    static boolean isAround(double dx, double dy, double dz, double radius) {
        return dx * dx + dy * dy + dz * dz <= radius * radius
                || inBox((int) Math.floor(dx), (int) Math.floor(dy), (int) Math.floor(dz));
    }

    /** Living entities of the given type at the monument (see {@link #contains}). Only sees loaded chunks. */
    public <T extends Entity> List<T> entities(Class<T> type) {
        // Search a box big enough for both the radius and the monument box, then keep the ones that count.
        double horizontal = Math.max(radius, -BOX_WEST + 1);
        double vertical = Math.max(radius, BOX_TOP + 1);
        return world.getNearbyEntitiesByType(type, center, horizontal, vertical, horizontal).stream()
                .filter(e -> !e.isDead() && contains(e.getLocation()))
                .toList();
    }

    /**
     * Whether a location is inside the monument itself: the 58x22x58 box of its four quadrants,
     * which also holds the wings and the top room.
     */
    public boolean inMonument(Location location) {
        return location.getWorld() == world && inBox(location.getBlockX() - center.getBlockX(),
                location.getBlockY() - center.getBlockY(), location.getBlockZ() - center.getBlockZ());
    }

    /** Whether a block offset from the center is in the monument box. */
    static boolean inBox(int dx, int dy, int dz) {
        return dx >= BOX_WEST && dx <= BOX_EAST && dy >= BOX_BOTTOM && dy <= BOX_TOP
                && dz >= BOX_WEST && dz <= BOX_EAST;
    }

    /** Counts the sponges (wet and dry) in the monument (see {@link #inMonument}). Its chunks must be loaded. */
    public int countSponges() {
        int count = 0;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = cx + BOX_WEST; x <= cx + BOX_EAST; x++) {
            for (int y = cy + BOX_BOTTOM; y <= cy + BOX_TOP; y++) {
                for (int z = cz + BOX_WEST; z <= cz + BOX_EAST; z++) {
                    Material type = world.getType(x, y, z);
                    if (type == Material.WET_SPONGE || type == Material.SPONGE) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Keeps the area's chunks loaded with plugin tickets (the datapack's {@code forceload add}) and
     * completes once their entities are loaded too, so the Elder Guardian count can be trusted.
     */
    public CompletableFuture<Void> loadChunks() {
        for (ChunkPos c : chunks) {
            world.addPluginChunkTicket(c.x(), c.z(), plugin);
        }

        CompletableFuture<Void> loaded = new CompletableFuture<>();
        BukkitTask[] task = new BukkitTask[1];
        int[] waited = {0};
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (allEntitiesLoaded()) {
                task[0].cancel();
                loaded.complete(null);
            } else if (++waited[0] >= ENTITY_LOAD_TIMEOUT_TICKS) {
                task[0].cancel();
                loaded.completeExceptionally(new TimeoutException(
                        "Monument chunks did not finish loading entities in time"));
            }
        }, 1L, 1L);
        return loaded;
    }

    /** Removes the plugin chunk tickets again (the datapack's {@code forceload remove}). */
    public void releaseChunks() {
        for (ChunkPos c : chunks) {
            world.removePluginChunkTicket(c.x(), c.z(), plugin);
        }
    }

    private boolean allEntitiesLoaded() {
        for (ChunkPos c : chunks) {
            if (!world.isChunkLoaded(c.x(), c.z()) || !world.getChunkAt(c.x(), c.z()).isEntitiesLoaded()) {
                return false;
            }
        }
        return true;
    }
}
