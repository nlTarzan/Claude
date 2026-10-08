package com.nltarzan.races.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

public final class Locations {

    private Locations() {
    }

    /**
     * The middle of the top of {@code block}, facing {@code yaw} snapped to the nearest 45° and looking
     * straight ahead (pitch 0). Used for every location set by right-clicking a block.
     */
    public static Location onTopOf(Block block, float yaw) {
        return new Location(block.getWorld(), block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5,
                snapYaw(yaw), 0f);
    }

    /** Rounds a yaw to the nearest multiple of 45° (S, SW, W, NW, N, NE, E, SE), kept within -180..180. */
    public static float snapYaw(float yaw) {
        float snapped = Math.round(yaw / 45f) * 45f;
        snapped = ((snapped + 180f) % 360f + 360f) % 360f - 180f;
        return snapped == -0f ? 0f : snapped;
    }

    public static String describe(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", "
                + location.getBlockZ();
    }
}
