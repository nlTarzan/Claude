package com.nltarzan.structures.monument.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Vanilla's room assignment: go through the units in random order and give every unused unit the
 * first room that fits. The difference with vanilla is that 2-wide rooms can run along X or Z
 * (random which is tried first), and their design is picked separately from the direction.
 */
final class RoomFitter {

    private static final List<GridDirection> AXES = List.of(GridDirection.EAST, GridDirection.BACK);

    private final MonumentGrid grid;
    private final RandomGenerator random;
    private final boolean[] claimed;
    private final List<Room> rooms = new ArrayList<>();

    private RoomFitter(MonumentGrid grid, RandomGenerator random) {
        this.grid = grid;
        this.random = random;
        this.claimed = new boolean[grid.cells().size()];
        for (Cell cell : grid.cells()) {
            claimed[cell.id()] = cell.isDummy();
        }
    }

    /**
     * @param coreAnchor the core's front-left bottom unit; the core takes up 2x2x2 units from there
     * @param sponges    units that must become sponge rooms (see {@link SpongeRooms}), claimed before
     *                   the other rooms so a tall room from below can't take them
     */
    static List<Room> fit(MonumentGrid grid, Cell coreAnchor, Set<Cell> sponges, RandomGenerator random) {
        RoomFitter fitter = new RoomFitter(grid, random);
        fitter.claim(new Room(RoomType.CORE, coreCells(grid, coreAnchor), null, false));
        fitter.claim(new Room(RoomType.ENTRY, List.of(grid.entry()), null, false));
        for (Cell sponge : sponges) {
            fitter.claim(new Room(RoomType.SPONGE, List.of(sponge), null, false));
        }

        List<Cell> order = new ArrayList<>(grid.roomCells());
        Collections.shuffle(order, random);
        for (Cell cell : order) {
            if (!fitter.claimed[cell.id()]) {
                fitter.fitRoom(cell);
            }
        }
        return List.copyOf(fitter.rooms);
    }

    /** The 8 units of the core, anchor first. */
    static List<Cell> coreCells(MonumentGrid grid, Cell anchor) {
        List<Cell> cells = new ArrayList<>(8);
        for (int floor = 0; floor < 2; floor++) {
            for (int row = 0; row < 2; row++) {
                for (int x = 0; x < 2; x++) {
                    cells.add(grid.at(anchor.x() + x, anchor.floor() + floor, anchor.row() + row));
                }
            }
        }
        return cells;
    }

    private void fitRoom(Cell cell) {
        Cell up = free(cell, GridDirection.UP);

        // 1-2. Vanilla DXY / DYZ: 2 wide and 2 tall.
        for (GridDirection axis : shuffledAxes()) {
            Cell side = free(cell, axis);
            Cell sideUp = side == null ? null : free(side, GridDirection.UP);
            if (side != null && up != null && sideUp != null) {
                claim(new Room(pick(RoomType.BIG_PIPES, RoomType.BIG_DARK), List.of(cell, side, up, sideUp), axis, false));
                return;
            }
        }
        // 3-4. Vanilla DZ / DX: 2 wide.
        for (GridDirection axis : shuffledAxes()) {
            Cell side = free(cell, axis);
            if (side != null) {
                claim(new Room(pick(RoomType.HALL_U, RoomType.HALL_LANTERN), List.of(cell, side), axis, false));
                return;
            }
        }
        // 5. Vanilla DY: 2 tall.
        if (up != null) {
            claim(new Room(RoomType.TALL, List.of(cell, up), null, false));
            return;
        }
        // 6. Sponge room: every wall closed except the floor.
        if (onlyFloorOpen(cell)) {
            claim(new Room(RoomType.SPONGE, List.of(cell), null, false));
            return;
        }
        // 7. Small room, 1/3 each. Four-pillar and dark rooms get a 50% chance of a center
        //    pillar when the floor and ceiling are closed and at least two walls are open.
        RoomType type = switch (random.nextInt(3)) {
            case 0 -> RoomType.SMALL_LANTERNS;
            case 1 -> RoomType.SMALL_PILLARS;
            default -> RoomType.SMALL_DARK;
        };
        boolean pillar = type != RoomType.SMALL_LANTERNS && random.nextBoolean()
                && !cell.isOpen(GridDirection.UP) && !cell.isOpen(GridDirection.DOWN) && cell.openCount() >= 2;
        claim(new Room(type, List.of(cell), null, pillar));
    }

    /** The neighbor in that direction if the wall to it is open and it's still unused, else null. */
    private Cell free(Cell cell, GridDirection direction) {
        Cell neighbor = cell.neighbor(direction);
        return neighbor != null && cell.isOpen(direction) && !claimed[neighbor.id()] ? neighbor : null;
    }

    private static boolean onlyFloorOpen(Cell cell) {
        for (GridDirection d : GridDirection.values()) {
            if (d != GridDirection.DOWN && cell.isOpen(d)) {
                return false;
            }
        }
        return true;
    }

    private List<GridDirection> shuffledAxes() {
        return random.nextBoolean() ? AXES : AXES.reversed();
    }

    private RoomType pick(RoomType a, RoomType b) {
        return random.nextBoolean() ? a : b;
    }

    private void claim(Room room) {
        for (Cell cell : room.cells()) {
            claimed[cell.id()] = true;
        }
        rooms.add(room);
    }
}
