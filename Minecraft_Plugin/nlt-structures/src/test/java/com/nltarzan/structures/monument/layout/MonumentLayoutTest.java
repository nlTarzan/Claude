package com.nltarzan.structures.monument.layout;

import com.nltarzan.structures.monument.layout.MonumentGrid.Wall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonumentLayoutTest {

    private static final int SEEDS = 3_000;

    @Test
    void gridMatchesVanilla() {
        MonumentGrid grid = new MonumentGrid();
        assertEquals(46, grid.roomCells().size());
        assertEquals(49, grid.cells().size());
        assertEquals(grid.at(2, 0, 0), grid.entry());

        Cell leftWing = grid.at(0, 1, -1);
        Cell rightWing = grid.at(4, 1, -1);
        Cell penthouse = grid.at(2, 3, 0);
        assertSame(grid.at(0, 1, 0), leftWing.neighbor(GridDirection.BACK));
        assertSame(grid.at(4, 1, 0), rightWing.neighbor(GridDirection.BACK));
        assertSame(grid.at(2, 2, 0), penthouse.neighbor(GridDirection.DOWN));
        for (Cell dummy : List.of(leftWing, rightWing, penthouse)) {
            long links = Arrays.stream(GridDirection.values()).filter(d -> dummy.neighbor(d) != null).count();
            assertEquals(1, links, dummy + " should touch exactly one unit");
        }
    }

    @Test
    void openWingHasAGroundFloorHalf() {
        MonumentGrid grid = new MonumentGrid(true, false);
        assertEquals(50, grid.cells().size());

        Cell upper = grid.at(0, 1, -1);
        Cell lower = grid.at(0, 0, -1);
        assertEquals(Cell.Kind.LEFT_WING, lower.kind());
        assertSame(grid.at(0, 0, 0), lower.neighbor(GridDirection.BACK));
        assertSame(upper, lower.neighbor(GridDirection.UP));
        assertSame(grid.at(0, 1, 0), upper.neighbor(GridDirection.BACK));
        // The closed right wing has no ground-floor half.
        assertEquals(null, grid.at(4, 0, -1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void openWingsUseBothFloors(double chance) {
        int groundOnly = 0;
        int firstOnly = 0;
        int both = 0;
        int closedWings = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentGrid grid = MonumentLayout.generate(seed, MazeSettings.DEFAULT.withNewestChance(chance)).grid();
            for (int x : new int[]{0, 4}) {
                Cell upper = grid.at(x, 1, -1);
                Cell lower = grid.at(x, 0, -1);
                if (lower == null) {
                    closedWings++;
                    assertTrue(upper.isOpen(GridDirection.BACK), chance + " seed " + seed + ": closed wing not connected");
                    continue;
                }
                boolean ground = lower.isOpen(GridDirection.BACK);
                boolean first = upper.isOpen(GridDirection.BACK);
                if (ground && first) {
                    both++;
                } else if (ground) {
                    groundOnly++;
                } else if (first) {
                    firstOnly++;
                }
            }
        }
        assertTrue(closedWings > SEEDS * 0.8 && closedWings < SEEDS * 1.2, chance + ": wings should be closed half the time");
        assertTrue(groundOnly > 0 && firstOnly > 0 && both > 0,
                chance + ": open wings should connect on the ground floor, the first floor and both, got "
                        + groundOnly + "/" + firstOnly + "/" + both);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.25, 0.5, 0.75, 1.0})
    void everyLayoutFollowsTheRules(double chance) {
        MazeSettings settings = MazeSettings.DEFAULT.withNewestChance(chance);
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentLayout layout = MonumentLayout.generate(seed, settings);
            String where = "chance " + chance + " seed " + seed;
            MonumentGrid grid = layout.grid();

            assertTrue(grid.allReachEntry(), where + ": every unit, wing and the penthouse must reach the entry");
            assertFalse(grid.entry().isOpen(GridDirection.UP), where + ": entry ceiling must stay closed");
            // No extra walls are opened: the layout is a tree, N cells joined by N - 1 open walls.
            assertEquals(grid.cells().size() - 1, openWalls(grid), where + ": layout should have no loops");
            long sponges = layout.rooms().stream().filter(r -> r.type() == RoomType.SPONGE).count();
            assertTrue(sponges >= 1, where + ": no sponge room");

            checkEveryUnitHasOneRoom(layout, where);
            for (Room room : layout.rooms()) {
                checkRoom(room, where);
            }
        }
    }

    @Test
    void newestChanceFollowsTheBellCurve() {
        int n = 20_000;
        double sum = 0;
        double sumSquares = 0;
        int withinOneSpread = 0;
        for (long seed = 0; seed < n; seed++) {
            double chance = MonumentLayout.generate(seed, MazeSettings.DEFAULT).newestChance();
            assertTrue(chance >= 0 && chance <= 1, "chance " + chance + " outside 0-1");
            sum += chance;
            sumSquares += chance * chance;
            if (Math.abs(chance - 0.65) <= 0.15) {
                withinOneSpread++;
            }
        }
        double mean = sum / n;
        double deviation = Math.sqrt(sumSquares / n - mean * mean);
        // Peak 0.65 and spread 0.15; the cut-off at 1.0 pulls the mean and spread down a little.
        assertTrue(mean > 0.62 && mean < 0.66, "mean " + mean);
        assertTrue(deviation > 0.13 && deviation < 0.16, "standard deviation " + deviation);
        double share = withinOneSpread / (double) n;
        assertTrue(share > 0.66 && share < 0.72, "share within peak +/- spread " + share);
    }

    @Test
    void newestChanceStaysWithinMinAndMax() {
        MazeSettings settings = new MazeSettings(0.65, 0.3, 0.5, 0.8, 1);
        for (long seed = 0; seed < 5_000; seed++) {
            double chance = MonumentLayout.generate(seed, settings).newestChance();
            assertTrue(chance >= 0.5 && chance <= 0.8, "chance " + chance + " outside 0.5-0.8");
        }
    }

    @Test
    void newestChanceGoesFromCorridorsToBranches() {
        // 1.0 is depth-first (long corridors, few dead ends), 0.0 is Prim-like (many dead ends).
        double newest = averageDeadEnds(1.0);
        double mixed = averageDeadEnds(0.5);
        double random = averageDeadEnds(0.0);
        assertTrue(newest < mixed && mixed < random,
                "dead ends should increase as the newest chance drops: " + newest + " / " + mixed + " / " + random);
    }

    @Test
    void layoutsWithoutAMinimumOftenLackSpongeRooms() {
        // Depth-first-like layouts regularly have no sponge room on their own (that's why the minimum exists).
        MazeSettings settings = MazeSettings.DEFAULT.withNewestChance(1.0).withMinSpongeRooms(0);
        long withoutSponges = 0;
        for (long seed = 0; seed < 500; seed++) {
            if (MonumentLayout.generate(seed, settings).rooms().stream().noneMatch(r -> r.type() == RoomType.SPONGE)) {
                withoutSponges++;
            }
        }
        assertTrue(withoutSponges > 250, "expected most layouts without sponges, got " + withoutSponges);
    }

    @Test
    void sameSeedGivesSameLayout() {
        for (long seed = 0; seed < 200; seed++) {
            assertEquals(LayoutPrinter.print(MonumentLayout.generate(seed, MazeSettings.DEFAULT)),
                    LayoutPrinter.print(MonumentLayout.generate(seed, MazeSettings.DEFAULT)));
        }
        assertNotEquals(LayoutPrinter.print(MonumentLayout.generate(1, MazeSettings.DEFAULT)),
                LayoutPrinter.print(MonumentLayout.generate(2, MazeSettings.DEFAULT)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void everyRoomTypeAndBothDirectionsShowUp(double chance) {
        Map<RoomType, Integer> counts = new EnumMap<>(RoomType.class);
        Set<String> directions = new HashSet<>();
        boolean pillar = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            for (Room room : MonumentLayout.generate(seed, MazeSettings.DEFAULT.withNewestChance(chance)).rooms()) {
                counts.merge(room.type(), 1, Integer::sum);
                if (room.axis() != null) {
                    directions.add(room.type() + " " + room.axis());
                }
                pillar |= room.centerPillar();
            }
        }
        for (RoomType roomType : RoomType.values()) {
            assertTrue(counts.getOrDefault(roomType, 0) > 0, chance + " never placed " + roomType);
        }
        for (RoomType wide : List.of(RoomType.BIG_PIPES, RoomType.BIG_DARK, RoomType.HALL_U, RoomType.HALL_LANTERN)) {
            assertTrue(directions.contains(wide + " EAST"), chance + ": " + wide + " never along X");
            assertTrue(directions.contains(wide + " BACK"), chance + ": " + wide + " never along Z");
        }
        assertTrue(pillar, chance + " never placed a center pillar");
    }

    private static double averageDeadEnds(double chance) {
        MazeSettings settings = MazeSettings.DEFAULT.withNewestChance(chance).withMinSpongeRooms(0);
        double deadEnds = 0;
        for (long seed = 0; seed < 1_000; seed++) {
            for (Cell cell : MonumentLayout.generate(seed, settings).grid().roomCells()) {
                if (cell.openCount() == 1) {
                    deadEnds++;
                }
            }
        }
        return deadEnds / 1_000;
    }

    private static void checkEveryUnitHasOneRoom(MonumentLayout layout, String where) {
        Map<Cell, Room> owner = new HashMap<>();
        for (Room room : layout.rooms()) {
            for (Cell cell : room.cells()) {
                assertFalse(cell.isDummy(), where + ": room on a dummy unit");
                Room previous = owner.put(cell, room);
                assertTrue(previous == null, where + ": " + cell + " is in two rooms");
            }
        }
        assertEquals(46, owner.size(), where + ": not every unit has a room");
        assertEquals(1, layout.rooms().stream().filter(r -> r.type() == RoomType.CORE).count(), where);
        assertEquals(1, layout.rooms().stream().filter(r -> r.type() == RoomType.ENTRY).count(), where);
    }

    private static void checkRoom(Room room, String where) {
        Cell a = room.anchor();
        String what = where + ": " + room.type() + " at " + a;
        switch (room.type()) {
            case CORE -> {
                assertEquals(8, room.cells().size(), what);
                assertEquals(0, a.floor(), what);
                assertEquals(2, a.row(), what);
                assertTrue(a.x() <= 3, what);
            }
            case ENTRY -> assertEquals(List.of(a), room.cells(), what);
            case BIG_PIPES, BIG_DARK -> {
                Cell side = a.neighbor(room.axis());
                Cell up = a.neighbor(GridDirection.UP);
                assertNotNull(side, what);
                assertNotNull(up, what);
                assertEquals(List.of(a, side, up, side.neighbor(GridDirection.UP)), room.cells(), what);
                assertTrue(a.isOpen(room.axis()) && a.isOpen(GridDirection.UP) && side.isOpen(GridDirection.UP),
                        what + ": built across a closed wall");
            }
            case HALL_U, HALL_LANTERN -> {
                assertEquals(List.of(a, a.neighbor(room.axis())), room.cells(), what);
                assertTrue(a.isOpen(room.axis()), what + ": built across a closed wall");
            }
            case TALL -> {
                assertEquals(List.of(a, a.neighbor(GridDirection.UP)), room.cells(), what);
                assertTrue(a.isOpen(GridDirection.UP), what + ": built across a closed wall");
            }
            case SPONGE -> {
                assertEquals(1, a.openCount(), what + ": sponge rooms only open at the floor");
                assertTrue(a.isOpen(GridDirection.DOWN), what);
            }
            case SMALL_LANTERNS, SMALL_PILLARS, SMALL_DARK -> {
                if (room.centerPillar()) {
                    assertNotEquals(RoomType.SMALL_LANTERNS, room.type(), what);
                    assertFalse(a.isOpen(GridDirection.UP) || a.isOpen(GridDirection.DOWN), what);
                    assertTrue(a.openCount() >= 2, what);
                }
            }
        }
        if (room.axis() == null) {
            assertTrue(room.type() != RoomType.HALL_U && room.type() != RoomType.HALL_LANTERN
                    && room.type() != RoomType.BIG_PIPES && room.type() != RoomType.BIG_DARK, what);
        }
    }

    private static int openWalls(MonumentGrid grid) {
        int open = 0;
        for (Wall wall : grid.walls()) {
            if (wall.cell().isOpen(wall.direction())) {
                open++;
            }
        }
        return open;
    }
}
