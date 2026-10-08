package com.nltarzan.structures.monument.layout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Draws a layout as text, one floor at a time, seen from above with the entrance at the bottom
 * (the replacement for the datapack's path.mcfunction). Units of the same room have no wall
 * between them. {@code ^} / {@code v} / {@code *} mark an opening up / down / both.
 */
public final class LayoutPrinter {

    private LayoutPrinter() {
    }

    public static List<String> print(MonumentLayout layout) {
        List<String> lines = new ArrayList<>();
        lines.add("Seed " + layout.seed() + " - growing tree, " + Math.round(layout.newestChance() * 100)
                + "% newest, left wing " + (layout.grid().isLeftWingOpen() ? "open" : "closed")
                + ", right wing " + (layout.grid().isRightWingOpen() ? "open" : "closed"));

        for (int floor = MonumentGrid.FLOORS - 1; floor >= 0; floor--) {
            lines.add("Floor " + floor + (floor == 2 ? " (top)" : floor == 0 ? " (bottom)" : ""));
            for (int row = MonumentGrid.ROWS - 1; row >= 0; row--) {
                addIfNotBlank(lines, border(layout, floor, row));
                addIfNotBlank(lines, contents(layout, floor, row));
            }
            addIfNotBlank(lines, border(layout, floor, -1));
        }
        lines.add(summary(layout));
        lines.add("C core, E entry, S sponge, T tall, U/L 2-wide hall (u/lantern), P/D 2x2 (pipes/dark),"
                + " l/i/d small (lanterns/pillars/dark), # center pillar. Gaps in the front corners lead to the wings"
                + " (open wings can connect on floor 0 too).");
        return lines;
    }

    /** Skips the empty lines of the rows the top floor doesn't have. */
    private static void addIfNotBlank(List<String> lines, String line) {
        if (!line.isBlank()) {
            lines.add(line);
        }
    }

    /** The line between {@code row + 1} (above) and {@code row} (below). */
    private static String border(MonumentLayout layout, int floor, int row) {
        StringBuilder line = new StringBuilder();
        for (int x = 0; x < MonumentGrid.WIDTH; x++) {
            line.append(corner(layout, x, floor, row));
            line.append(connected(layout, at(layout, x, floor, row + 1), at(layout, x, floor, row)) ? "   " : "---");
        }
        return line.append(corner(layout, MonumentGrid.WIDTH, floor, row)).toString().stripTrailing();
    }

    private static String contents(MonumentLayout layout, int floor, int row) {
        StringBuilder line = new StringBuilder();
        for (int x = 0; x <= MonumentGrid.WIDTH; x++) {
            Cell left = at(layout, x - 1, floor, row);
            Cell cell = at(layout, x, floor, row);
            line.append(connected(layout, left, cell) ? ' ' : '|');
            if (x < MonumentGrid.WIDTH) {
                line.append(cell == null ? "   " : label(layout, cell));
            }
        }
        return line.toString().stripTrailing();
    }

    private static String label(MonumentLayout layout, Cell cell) {
        Room room = layout.roomOf(cell);
        boolean up = opening(layout, cell, GridDirection.UP);
        boolean down = opening(layout, cell, GridDirection.DOWN);
        char marker = up && down ? '*' : up ? '^' : down ? 'v' : ' ';
        return " " + room.type().label() + (room.centerPillar() ? '#' : marker);
    }

    /** True when there's no wall to draw: open to a different room, or part of the same room. Blank when neither exists. */
    private static boolean connected(MonumentLayout layout, Cell a, Cell b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return layout.roomOf(a) == layout.roomOf(b) || isOpenBetween(a, b);
    }

    private static boolean isOpenBetween(Cell a, Cell b) {
        for (GridDirection d : GridDirection.values()) {
            if (a.neighbor(d) == b) {
                return a.isOpen(d);
            }
        }
        return false;
    }

    private static boolean opening(MonumentLayout layout, Cell cell, GridDirection direction) {
        Cell other = cell.neighbor(direction);
        return other != null && cell.isOpen(direction) && layout.roomOf(other) != layout.roomOf(cell);
    }

    private static char corner(MonumentLayout layout, int x, int floor, int row) {
        boolean any = exists(layout, x - 1, floor, row) || exists(layout, x, floor, row)
                || exists(layout, x - 1, floor, row + 1) || exists(layout, x, floor, row + 1);
        return any ? '+' : ' ';
    }

    private static boolean exists(MonumentLayout layout, int x, int floor, int row) {
        Cell cell = layout.grid().at(x, floor, row);
        return cell != null && !cell.isDummy();
    }

    /** A cell at that position, dummies included (so wing gaps show in the front border). */
    private static Cell at(MonumentLayout layout, int x, int floor, int row) {
        return layout.grid().at(x, floor, row);
    }

    private static String summary(MonumentLayout layout) {
        Map<RoomType, Integer> counts = new EnumMap<>(RoomType.class);
        for (Room room : layout.rooms()) {
            counts.merge(room.type(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(e -> e.getValue() + "x " + e.getKey().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", ", "Rooms: ", ""));
    }
}
