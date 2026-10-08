package com.nltarzan.races.race;

import org.bukkit.Location;

/**
 * A spot in a points race; the first player within {@code radius} blocks of its center scores.
 * Only the position matters, so any facing direction is dropped.
 */
public record PointZone(Location center, double radius) {

    public PointZone {
        center = new Location(center.getWorld(), center.getX(), center.getY(), center.getZ());
    }

    @Override
    public Location center() {
        return center.clone();
    }
}
