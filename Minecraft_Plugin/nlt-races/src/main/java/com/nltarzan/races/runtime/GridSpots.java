package com.nltarzan.races.runtime;

import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.Region;
import com.nltarzan.races.util.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The starting positions of a group or points race, in the order they're handed out (leader first). */
final class GridSpots {

    /** Blocks between positions when spreading players over a grid area. */
    private static final int AREA_SPACING = 2;

    private GridSpots() {
    }

    static List<Location> of(Race race) {
        List<Location> spots = race.getGridSlots().isEmpty() ? fromArea(race) : copy(race.getGridSlots());
        int max = Math.min(spots.size(), race.getGridMax());
        return new ArrayList<>(spots.subList(0, max));
    }

    private static List<Location> copy(List<Location> locations) {
        return locations.stream().map(Location::clone).toList();
    }

    /**
     * Positions every {@value #AREA_SPACING} blocks on top of the grid area's bottom layer (the floor that
     * was clicked), closest to the start checkpoint first, all facing it (snapped to 45°).
     */
    private static List<Location> fromArea(Race race) {
        Region area = race.getGridRegion();
        World world = area == null ? null : Bukkit.getWorld(area.world());
        if (world == null) {
            return List.of();
        }
        Location target = center(race.getStartCheckpoint() != null ? race.getStartCheckpoint() : area, world);
        List<Location> spots = new ArrayList<>();
        for (int x = area.minX(); x <= area.maxX(); x += AREA_SPACING) {
            for (int z = area.minZ(); z <= area.maxZ(); z += AREA_SPACING) {
                Location spot = new Location(world, x + 0.5, area.minY() + 1, z + 0.5);
                spot.setYaw(Locations.snapYaw(yawTowards(spot, target)));
                spots.add(spot);
            }
        }
        spots.sort(Comparator.comparingDouble(spot -> spot.distanceSquared(target)));
        return spots;
    }

    private static Location center(Region region, World world) {
        return new Location(world, (region.minX() + region.maxX() + 1) / 2.0, region.minY(),
                (region.minZ() + region.maxZ() + 1) / 2.0);
    }

    private static float yawTowards(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
