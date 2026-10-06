package com.nltarzan.structures.monument.layout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The vanilla room grid: two 5x4 floors and a 3x2 top floor (46 units), plus dummy units for the
 * wings and the penthouse. Every wall starts closed.
 *
 * <p>A closed wing ({@code wing_closed}) is one dummy in front of the first floor's front corner,
 * like vanilla. An open wing ({@code wing_open}) gets a second dummy below it, in front of the
 * ground floor's corner, so it can open into the monument on either floor or both (the datapack's
 * wing nodes at ~ and ~4).
 */
public final class MonumentGrid {

    public static final int WIDTH = 5;
    public static final int ROWS = 4;
    public static final int FLOORS = 3;

    /** A wall between {@code cell} and its neighbor in {@code direction}. */
    public record Wall(Cell cell, GridDirection direction) {
        public Cell other() {
            return cell.neighbor(direction);
        }
    }

    private final List<Cell> cells = new ArrayList<>();
    private final List<Cell> roomCells = new ArrayList<>();
    private final Map<Long, Cell> byPosition = new HashMap<>();
    private final Cell entry;
    private final boolean leftWingOpen;
    private final boolean rightWingOpen;

    /** A grid with two closed wings, like vanilla. */
    public MonumentGrid() {
        this(false, false);
    }

    public MonumentGrid(boolean leftWingOpen, boolean rightWingOpen) {
        this.leftWingOpen = leftWingOpen;
        this.rightWingOpen = rightWingOpen;
        for (int floor = 0; floor < FLOORS; floor++) {
            for (int row = 0; row < ROWS; row++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (isRoomPosition(x, floor, row)) {
                        roomCells.add(add(x, floor, row, Cell.Kind.ROOM));
                    }
                }
            }
        }
        // The wings sit in front of the front corners, the penthouse above the top floor's front center.
        // At these positions each dummy only touches its own unit (and an open wing's other half).
        add(0, 1, -1, Cell.Kind.LEFT_WING);
        add(4, 1, -1, Cell.Kind.RIGHT_WING);
        if (leftWingOpen) {
            add(0, 0, -1, Cell.Kind.LEFT_WING);
        }
        if (rightWingOpen) {
            add(4, 0, -1, Cell.Kind.RIGHT_WING);
        }
        add(2, 3, 0, Cell.Kind.PENTHOUSE);

        for (Cell cell : cells) {
            for (GridDirection d : GridDirection.values()) {
                cell.neighbors[d.ordinal()] = at(cell.x() + d.dx(), cell.floor() + d.dFloor(), cell.row() + d.dRow());
            }
        }

        entry = at(2, 0, 0);
        // Like the datapack: the entry room has no opening in its ceiling.
        block(entry, GridDirection.UP);
    }

    public static boolean isRoomPosition(int x, int floor, int row) {
        if (floor == 0 || floor == 1) {
            return x >= 0 && x < WIDTH && row >= 0 && row < ROWS;
        }
        return floor == 2 && x >= 1 && x <= 3 && row >= 0 && row <= 1;
    }

    /** Whether the left (west) wing is {@code wing_open}, else {@code wing_closed}. */
    public boolean isLeftWingOpen() {
        return leftWingOpen;
    }

    /** Whether the right (east) wing is {@code wing_open}, else {@code wing_closed}. */
    public boolean isRightWingOpen() {
        return rightWingOpen;
    }

    /** All cells, dummies included. */
    public List<Cell> cells() {
        return Collections.unmodifiableList(cells);
    }

    /** The 46 real units, without the dummies. */
    public List<Cell> roomCells() {
        return Collections.unmodifiableList(roomCells);
    }

    public Cell entry() {
        return entry;
    }

    /** The cell at a grid position (dummies included), or {@code null}. */
    public Cell at(int x, int floor, int row) {
        return byPosition.get(key(x, floor, row));
    }

    /** Whether the wall can ever be open: a neighbor exists and the wall isn't blocked. */
    public boolean canOpen(Cell cell, GridDirection direction) {
        return cell.neighbor(direction) != null && !cell.blocked[direction.ordinal()];
    }

    /** Opens or closes a wall on both sides. Returns false when the wall can't be opened. */
    public boolean setOpen(Cell cell, GridDirection direction, boolean open) {
        if (open && !canOpen(cell, direction)) {
            return false;
        }
        Cell other = cell.neighbor(direction);
        if (other == null) {
            return !open;
        }
        cell.open[direction.ordinal()] = open;
        other.open[direction.opposite().ordinal()] = open;
        return true;
    }

    /** Opens (or closes) every wall that can be opened. */
    public void setAllOpen(boolean open) {
        for (Cell cell : cells) {
            for (GridDirection d : GridDirection.values()) {
                if (canOpen(cell, d)) {
                    setOpen(cell, d, open);
                }
            }
        }
    }

    /** Every wall between two cells, each listed once, open or closed. */
    public List<Wall> walls() {
        List<Wall> walls = new ArrayList<>();
        for (Cell cell : cells) {
            for (GridDirection d : List.of(GridDirection.EAST, GridDirection.BACK, GridDirection.UP)) {
                if (cell.neighbor(d) != null) {
                    walls.add(new Wall(cell, d));
                }
            }
        }
        return walls;
    }

    /** Whether a path through open walls leads from {@code from} to the entry. */
    public boolean reachesEntry(Cell from) {
        return reachableFrom(from)[entry.id()];
    }

    /** Whether every cell, dummies included, can reach the entry through open walls. */
    public boolean allReachEntry() {
        boolean[] reached = reachableFrom(entry);
        for (Cell cell : cells) {
            if (!reached[cell.id()]) {
                return false;
            }
        }
        return true;
    }

    /** Which cells (by id) can reach the entry through open walls. */
    public boolean[] reachableFromEntry() {
        return reachableFrom(entry);
    }

    /** Number of steps through open walls from the entry to every cell (by id), -1 when unreachable. */
    public int[] distancesFromEntry() {
        int[] distance = new int[cells.size()];
        Arrays.fill(distance, -1);
        Deque<Cell> queue = new ArrayDeque<>();
        distance[entry.id()] = 0;
        queue.add(entry);
        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            for (GridDirection d : GridDirection.values()) {
                Cell n = cell.neighbor(d);
                if (n != null && cell.isOpen(d) && distance[n.id()] < 0) {
                    distance[n.id()] = distance[cell.id()] + 1;
                    queue.add(n);
                }
            }
        }
        return distance;
    }

    private boolean[] reachableFrom(Cell start) {
        boolean[] seen = new boolean[cells.size()];
        Deque<Cell> queue = new ArrayDeque<>();
        seen[start.id()] = true;
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            for (GridDirection d : GridDirection.values()) {
                Cell n = cell.neighbor(d);
                if (n != null && cell.isOpen(d) && !seen[n.id()]) {
                    seen[n.id()] = true;
                    queue.add(n);
                }
            }
        }
        return seen;
    }

    private void block(Cell cell, GridDirection direction) {
        Cell other = cell.neighbor(direction);
        setOpen(cell, direction, false);
        cell.blocked[direction.ordinal()] = true;
        if (other != null) {
            other.blocked[direction.opposite().ordinal()] = true;
        }
    }

    private Cell add(int x, int floor, int row, Cell.Kind kind) {
        Cell cell = new Cell(cells.size(), x, floor, row, kind);
        cells.add(cell);
        byPosition.put(key(x, floor, row), cell);
        return cell;
    }

    private static long key(int x, int floor, int row) {
        return ((long) (x + 16) << 32) | ((long) (floor + 16) << 16) | (row + 16);
    }
}
