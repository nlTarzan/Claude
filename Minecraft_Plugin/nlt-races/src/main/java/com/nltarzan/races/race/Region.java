package com.nltarzan.races.race;

import org.bukkit.Location;

/** A box of blocks (both corners included), used for checkpoints and grid regions. */
public record Region(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /** Blocks between the points checked along a player's path each tick. */
    private static final double SWEEP_STEP = 0.25;

    public static Region of(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            throw new IllegalArgumentException("Both corners must be in the same world");
        }
        return new Region(a.getWorld().getName(),
                Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(), b.getBlockY()),
                Math.min(a.getBlockZ(), b.getBlockZ()), Math.max(a.getBlockX(), b.getBlockX()),
                Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
    }

    public boolean contains(String worldName, double x, double y, double z) {
        return world.equals(worldName)
                && x >= minX && x < maxX + 1
                && y >= minY && y < maxY + 1
                && z >= minZ && z < maxZ + 1;
    }

    public boolean contains(Location location) {
        return location.getWorld() != null
                && contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    /**
     * Checks the straight path a player moved between two ticks and returns how far along it (0..1]
     * they entered this region, or -1 if they didn't enter it. Checking the whole path means fast
     * racers can't skip over a thin checkpoint, and the fraction gives sub-tick timing.
     * Starting inside the region doesn't count as entering it.
     */
    public double firstEntry(Location from, Location to) {
        if (to.getWorld() == null || !to.getWorld().getName().equals(world)
                || !to.getWorld().equals(from.getWorld()) || contains(from)) {
            return -1;
        }
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / SWEEP_STEP));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (contains(world, from.getX() + dx * t, from.getY() + dy * t, from.getZ() + dz * t)) {
                return t;
            }
        }
        return -1;
    }

    /** Stored in yml as "world,minX,minY,minZ,maxX,maxY,maxZ". */
    public String serialize() {
        return world + "," + minX + "," + minY + "," + minZ + "," + maxX + "," + maxY + "," + maxZ;
    }

    public static Region parse(String raw) {
        String[] p = raw.split(",");
        if (p.length != 7) {
            throw new IllegalArgumentException("Invalid region '" + raw + "'");
        }
        return new Region(p[0].trim(), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()),
                Integer.parseInt(p[3].trim()), Integer.parseInt(p[4].trim()), Integer.parseInt(p[5].trim()),
                Integer.parseInt(p[6].trim()));
    }

    public String describe() {
        return String.format("%s %d, %d, %d → %d, %d, %d", world, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
