package com.nltarzan.structures.monument.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Growing Tree maze: keep a list of units that are part of the maze and may still have unvisited
 * neighbors. Every step picks one of them, opens the wall to a random unvisited neighbor and adds
 * that neighbor to the list; a unit without unvisited neighbors left is removed from the list.
 *
 * <p>Which unit is picked decides the maze's character. With chance {@code newestChance} it's the
 * newest unit, otherwise a random one from the list:
 * <ul>
 *     <li>1.0: always the newest, the same as depth-first: long winding corridors, few dead ends</li>
 *     <li>0.0: always random, like Prim's: lots of short branches and dead ends</li>
 *     <li>in between: winding corridors with side branches</li>
 * </ul>
 */
final class GrowingTreeGenerator {

    private GrowingTreeGenerator() {
    }

    static void carve(MonumentGrid grid, RandomGenerator random, double newestChance) {
        grid.setAllOpen(false);
        boolean[] visited = new boolean[grid.cells().size()];
        List<Cell> active = new ArrayList<>();

        List<Cell> starts = grid.roomCells();
        Cell start = starts.get(random.nextInt(starts.size()));
        visited[start.id()] = true;
        active.add(start);

        List<GridDirection> options = new ArrayList<>(6);
        while (!active.isEmpty()) {
            int index = random.nextDouble() < newestChance ? active.size() - 1 : random.nextInt(active.size());
            Cell current = active.get(index);

            options.clear();
            for (GridDirection d : GridDirection.values()) {
                if (grid.canOpen(current, d) && !visited[current.neighbor(d).id()]) {
                    options.add(d);
                }
            }
            if (options.isEmpty()) {
                active.remove(index);
                continue;
            }

            GridDirection step = options.get(random.nextInt(options.size()));
            Cell next = current.neighbor(step);
            grid.setOpen(current, step, true);
            visited[next.id()] = true;
            active.add(next);
        }
    }
}
