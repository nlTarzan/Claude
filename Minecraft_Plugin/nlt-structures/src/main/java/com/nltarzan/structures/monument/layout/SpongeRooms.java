package com.nltarzan.structures.monument.layout;

import com.nltarzan.structures.monument.layout.MonumentGrid.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Makes sure a layout gets its minimum number of sponge rooms. A sponge room must only be open
 * at the floor, so this picks a unit that isn't on the bottom floor and rewires it: all its walls
 * are closed except the floor.
 *
 * <p>Preferred are dead ends far from the entry, because rewiring those changes nothing else. When
 * no such unit exists, a unit in the middle of the maze is used, and every part of the maze that
 * got cut off by closing its walls is reconnected through a random other wall.
 */
final class SpongeRooms {

    private SpongeRooms() {
    }

    /**
     * Rewires one unit into a sponge room spot.
     *
     * @param excluded units that can't become a sponge room (core and entry)
     * @param sponges  sponge room spots chosen earlier, left untouched
     * @return the unit, or {@code null} when no unit qualifies
     */
    static Cell makeSpongeSpot(MonumentGrid grid, Set<Cell> excluded, Set<Cell> sponges, RandomGenerator random) {
        int[] distance = grid.distancesFromEntry();
        List<Cell> candidates = new ArrayList<>();
        for (Cell cell : grid.roomCells()) {
            Cell below = cell.neighbor(GridDirection.DOWN);
            if (below != null && grid.canOpen(cell, GridDirection.DOWN)
                    && !excluded.contains(cell) && !sponges.contains(cell) && !sponges.contains(below)) {
                candidates.add(cell);
            }
        }
        // Dead ends first (rewiring one never removes a loop), then the furthest from the entry.
        candidates.sort(Comparator.comparing((Cell c) -> c.openCount() != 1)
                .thenComparing(Comparator.comparingInt((Cell c) -> distance[c.id()]).reversed()));

        for (boolean allowRepair : new boolean[]{false, true}) {
            for (Cell cell : candidates) {
                if (tryRewire(grid, cell, sponges, allowRepair, random)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static boolean tryRewire(MonumentGrid grid, Cell cell, Set<Cell> sponges, boolean allowRepair,
                                     RandomGenerator random) {
        boolean[] before = new boolean[GridDirection.values().length];
        for (GridDirection d : GridDirection.values()) {
            before[d.ordinal()] = cell.isOpen(d);
            if (d != GridDirection.DOWN && cell.neighbor(d) != null) {
                grid.setOpen(cell, d, false);
            }
        }
        grid.setOpen(cell, GridDirection.DOWN, true);

        List<Wall> repairs = new ArrayList<>();
        if (grid.allReachEntry() || (allowRepair && reconnect(grid, cell, sponges, repairs, random))) {
            return true;
        }

        for (Wall wall : repairs) {
            grid.setOpen(wall.cell(), wall.direction(), false);
        }
        for (GridDirection d : GridDirection.values()) {
            if (cell.neighbor(d) != null) {
                grid.setOpen(cell, d, before[d.ordinal()]);
            }
        }
        return false;
    }

    /**
     * Opens random walls from the part that reaches the entry into the cut-off parts until
     * everything is connected again. Never opens a wall of a sponge room spot.
     */
    private static boolean reconnect(MonumentGrid grid, Cell spot, Set<Cell> sponges, List<Wall> opened,
                                     RandomGenerator random) {
        while (true) {
            boolean[] reached = grid.reachableFromEntry();
            if (allTrue(reached, grid)) {
                return true;
            }
            List<Wall> options = new ArrayList<>();
            for (Wall wall : grid.walls()) {
                Cell a = wall.cell();
                Cell b = wall.other();
                if (reached[a.id()] != reached[b.id()] && !a.isOpen(wall.direction())
                        && grid.canOpen(a, wall.direction())
                        && a != spot && b != spot && !sponges.contains(a) && !sponges.contains(b)) {
                    options.add(wall);
                }
            }
            if (options.isEmpty()) {
                return false;
            }
            Wall wall = options.get(random.nextInt(options.size()));
            grid.setOpen(wall.cell(), wall.direction(), true);
            opened.add(wall);
        }
    }

    private static boolean allTrue(boolean[] reached, MonumentGrid grid) {
        for (Cell cell : grid.cells()) {
            if (!reached[cell.id()]) {
                return false;
            }
        }
        return true;
    }
}
