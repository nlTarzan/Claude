package com.nltarzan.structures.monument.layout;

import java.util.List;

/**
 * Directions on the room grid, seen from the entrance looking into the monument:
 * EAST is right, BACK is deeper into the monument (north in the datapack), UP is the next floor.
 */
public enum GridDirection {
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    BACK(0, 0, 1),
    FRONT(0, 0, -1),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    public static final List<GridDirection> HORIZONTAL = List.of(EAST, WEST, BACK, FRONT);

    private final int dx;
    private final int dFloor;
    private final int dRow;

    GridDirection(int dx, int dFloor, int dRow) {
        this.dx = dx;
        this.dFloor = dFloor;
        this.dRow = dRow;
    }

    public int dx() {
        return dx;
    }

    public int dFloor() {
        return dFloor;
    }

    public int dRow() {
        return dRow;
    }

    public GridDirection opposite() {
        return switch (this) {
            case EAST -> WEST;
            case WEST -> EAST;
            case BACK -> FRONT;
            case FRONT -> BACK;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }
}
