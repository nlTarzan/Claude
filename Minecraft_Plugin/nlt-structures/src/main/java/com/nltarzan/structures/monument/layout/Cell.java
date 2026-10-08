package com.nltarzan.structures.monument.layout;

/**
 * One 8x8x4 unit of the monument (a "node" in the datapack), or one of the three dummy units
 * standing in for the wings and the penthouse. Walls are changed through {@link MonumentGrid}
 * so both sides of a wall always agree.
 */
public final class Cell {

    public enum Kind {
        ROOM,
        LEFT_WING,
        RIGHT_WING,
        PENTHOUSE
    }

    private final int id;
    private final int x;
    private final int floor;
    private final int row;
    private final Kind kind;

    final Cell[] neighbors = new Cell[GridDirection.values().length];
    final boolean[] open = new boolean[GridDirection.values().length];
    final boolean[] blocked = new boolean[GridDirection.values().length];

    Cell(int id, int x, int floor, int row, Kind kind) {
        this.id = id;
        this.x = x;
        this.floor = floor;
        this.row = row;
        this.kind = kind;
    }

    public int id() {
        return id;
    }

    /** 0 (left/west) to 4 (right/east). */
    public int x() {
        return x;
    }

    /** 0 (bottom) to 2 (top). */
    public int floor() {
        return floor;
    }

    /** 0 (front, entrance side) to 3 (back). */
    public int row() {
        return row;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isDummy() {
        return kind != Kind.ROOM;
    }

    public Cell neighbor(GridDirection direction) {
        return neighbors[direction.ordinal()];
    }

    public boolean isOpen(GridDirection direction) {
        return open[direction.ordinal()];
    }

    public int openCount() {
        int count = 0;
        for (boolean o : open) {
            if (o) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return isDummy() ? kind.name() : "(" + x + "," + floor + "," + row + ")";
    }
}
