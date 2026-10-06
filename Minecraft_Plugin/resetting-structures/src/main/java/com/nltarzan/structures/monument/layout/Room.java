package com.nltarzan.structures.monument.layout;

import java.util.List;

/**
 * A placed room.
 *
 * @param cells        the units it takes up; the first one is the anchor (lowest x, floor and row)
 * @param axis         {@link GridDirection#EAST} or {@link GridDirection#BACK} for 2-wide rooms, else {@code null}
 * @param centerPillar small rooms only: add the 2x2 center pillar
 */
public record Room(RoomType type, List<Cell> cells, GridDirection axis, boolean centerPillar) {

    public Room {
        cells = List.copyOf(cells);
    }

    public Cell anchor() {
        return cells.getFirst();
    }
}
