package com.nltarzan.structures.monument.build;

import com.nltarzan.structures.monument.build.BuildStep.Block;
import com.nltarzan.structures.monument.build.BuildStep.Fill;
import com.nltarzan.structures.monument.build.BuildStep.Offset;
import com.nltarzan.structures.monument.build.BuildStep.Place;
import com.nltarzan.structures.monument.build.BuildStep.Rotation;
import com.nltarzan.structures.monument.build.BuildStep.SpawnElder;
import com.nltarzan.structures.monument.layout.Cell;
import com.nltarzan.structures.monument.layout.GridDirection;
import com.nltarzan.structures.monument.layout.MazeSettings;
import com.nltarzan.structures.monument.layout.MonumentGrid;
import com.nltarzan.structures.monument.layout.MonumentLayout;
import com.nltarzan.structures.monument.layout.Room;
import com.nltarzan.structures.monument.layout.RoomType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPlannerTest {

    /** Template sizes (x, z) as saved in the .nbt files, before rotating. */
    private static final Map<String, int[]> SIZES = Map.ofEntries(
            Map.entry("room/1x1x1_dark", new int[]{8, 8}),
            Map.entry("room/1x1x1_lanterns", new int[]{8, 8}),
            Map.entry("room/1x1x1_pillars", new int[]{8, 8}),
            Map.entry("room/1x1x1_sponge", new int[]{8, 8}),
            Map.entry("room/1x1x1_start", new int[]{8, 8}),
            Map.entry("room/1x1x2", new int[]{8, 8}),
            Map.entry("room/2x1x1_lantern", new int[]{16, 8}),
            Map.entry("room/2x1x1_u", new int[]{16, 8}),
            Map.entry("room/2x1x2_dark", new int[]{16, 8}),
            Map.entry("room/2x1x2_pipes", new int[]{16, 8}),
            Map.entry("room/2x2x2_core", new int[]{16, 16}));

    private static final int SEEDS = 2_000;

    @Test
    void nodesMatchTheDatapackMarkers() {
        MonumentGrid grid = new MonumentGrid(true, true);
        assertEquals(new Offset(0, 0, 3), BuildPlanner.node(grid.entry()));                // Entrance
        assertEquals(new Offset(-16, 0, -21), BuildPlanner.node(grid.at(0, 0, 3)));         // back-left Core node
        assertEquals(new Offset(8, 8, -5), BuildPlanner.node(grid.at(3, 2, 1)));            // 2nd floor
        assertEquals(new Offset(16, 4, 11), BuildPlanner.node(grid.at(4, 1, -1)));          // east Wing
        assertEquals(new Offset(-16, 0, 11), BuildPlanner.node(grid.at(0, 0, -1)));         // open west Wing, ground
        assertEquals(new Offset(0, 12, 3), BuildPlanner.node(grid.at(2, 3, 0)));            // Penthouse
    }

    @Test
    void rotatedHallsMatchTheDatapackOffsets() {
        // west.mcfunction: at the west node W, "place template 2x1x1_lantern ~11.5 ~ ~3.5 180".
        Offset w = new Offset(-8, 0, -5);
        assertEquals(w.plus(11, 0, 3), BuildPlanner.rotatedOrigin(w.plus(-4, 0, -4), 16, 8, Rotation.CLOCKWISE_180));
        // north.mcfunction: at the north node N, "~-3.5 ~ ~11.5 counterclockwise_90".
        Offset n = new Offset(0, 0, -13);
        assertEquals(n.plus(-4, 0, 11), BuildPlanner.rotatedOrigin(n.plus(-4, 0, -4), 8, 16, Rotation.COUNTERCLOCKWISE_90));
        // south.mcfunction: at the south node S, "~3.5 ~ ~-11.5 clockwise_90" (the room runs north of S).
        Offset s = new Offset(0, 0, -5);
        assertEquals(s.plus(3, 0, -12), BuildPlanner.rotatedOrigin(s.plus(-4, 0, -12), 8, 16, Rotation.CLOCKWISE_90));
    }

    @Test
    void everyRoomTemplateCoversExactlyItsUnits() {
        Set<Rotation> rotationsUsed = new HashSet<>();
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentLayout layout = MonumentLayout.generate(seed, MazeSettings.DEFAULT);
            SplittableRandom random = new SplittableRandom(seed);
            for (Room room : layout.rooms()) {
                Place place = BuildPlanner.placeRoom(room, random);
                rotationsUsed.add(place.rotation());
                int[] size = SIZES.get(place.template());
                int[] box = coveredBox(place.origin(), size[0], size[1], place.rotation());
                int[] units = unitBox(room);
                assertEquals(List.of(units[0], units[1], units[2], units[3]), List.of(box[0], box[1], box[2], box[3]),
                        "seed " + seed + ": " + room.type() + " " + place.rotation() + " doesn't cover its units");
            }
        }
        assertEquals(Set.of(Rotation.values()), rotationsUsed, "2-wide rooms should use all four rotations");
    }

    @Test
    void roomsDontOverlapAndStayInsideTheShell() {
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentLayout layout = MonumentLayout.generate(seed, MazeSettings.DEFAULT);
            Set<String> taken = new HashSet<>();
            for (Room room : layout.rooms()) {
                int[] box = unitBox(room);
                assertTrue(box[0] >= -29 && box[1] <= 28 && box[2] >= -29 && box[3] <= 28, "seed " + seed + ": outside");
                for (Cell cell : room.cells()) {
                    Offset node = BuildPlanner.node(cell);
                    assertTrue(taken.add(node.x() + "," + node.y() + "," + node.z()), "seed " + seed + ": overlap");
                }
            }
        }
    }

    @Test
    void planHasEverythingInTheRightOrder() {
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentLayout layout = MonumentLayout.generate(seed, MazeSettings.DEFAULT);
            List<BuildStep> steps = BuildPlanner.plan(layout);
            String where = "seed " + seed;

            List<String> templates = steps.stream()
                    .filter(s -> s instanceof Place).map(s -> ((Place) s).template()).toList();
            assertEquals(List.of("monument_empty_nw", "monument_empty_ne", "monument_empty_sw", "monument_empty_se"),
                    templates.subList(0, 4), where);
            long roomPlacements = templates.stream().filter(t -> t.startsWith("room/1x") || t.startsWith("room/2x")).count();
            assertEquals(layout.rooms().size(), roomPlacements, where + ": every room once");
            assertEquals(3, steps.stream().filter(s -> s instanceof SpawnElder).count(), where);

            long vertical = layout.openings().stream().filter(w -> w.direction() == GridDirection.UP).count();
            assertEquals(vertical, templates.stream().filter("roof_open"::equals).count(), where);

            // The floors come before the first room, the doorways after the last one.
            int firstFloor = indexOf(steps, s -> s instanceof Fill f && f.block() == Block.PRISMARINE);
            int firstRoom = indexOf(steps, s -> s instanceof Place p && p.template().equals("room/2x2x2_core"));
            assertTrue(firstFloor < firstRoom, where);
            String wings = layout.grid().isRightWingOpen() + "/" + layout.grid().isLeftWingOpen();
            assertEquals(wings, templates.get(5).equals("room/wing_open") + "/" + templates.get(6).equals("room/wing_open"), where);
        }
    }

    @Test
    void doorwaysAreTwoByTwoThroughBothWalls() {
        // horizontal_connecters.mcfunction for a node P whose parent is north: "fill ~-0.5 ~ ~-2.5 ~0.5 ~1 ~-5.5 water".
        Offset p = new Offset(8, 4, -5);
        Fill north = BuildPlanner.doorway(p, p.plus(0, 0, -8));
        assertEquals(new Fill(p.plus(-1, 0, -6), p.plus(0, 1, -3), Block.WATER), north);
        // ...and for a parent to the east: "fill ~2.5 ~ ~-0.5 ~5.5 ~1 ~0.5 water".
        Fill east = BuildPlanner.doorway(p, p.plus(8, 0, 0));
        assertEquals(new Fill(p.plus(2, 0, -1), p.plus(5, 1, 0), Block.WATER), east);
        // The same doorway no matter which side it's seen from.
        assertEquals(north, BuildPlanner.doorway(p.plus(0, 0, -8), p));
        assertEquals(east, BuildPlanner.doorway(p.plus(8, 0, 0), p));
    }

    @Test
    void spongeColumnsFollowVanillaChances() {
        int columns = 0;
        int empty = 0;
        int two = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            MonumentLayout layout = MonumentLayout.generate(seed, MazeSettings.DEFAULT);
            for (BuildStep step : BuildPlanner.plan(layout)) {
                if (step instanceof Fill f && f.block() == Block.WET_SPONGE) {
                    columns++;
                    if (f.from().y() != f.to().y()) {
                        two++;
                    }
                }
            }
            long spongeRooms = layout.rooms().stream().filter(r -> r.type() == RoomType.SPONGE).count();
            empty += (int) (spongeRooms * 36);
        }
        empty -= columns;
        double total = empty + columns;
        assertTrue(Math.abs(empty / total - 1 / 3.0) < 0.02, "empty share " + empty / total);
        assertTrue(Math.abs(two / total - 1 / 6.0) < 0.02, "two-sponge share " + two / total);
    }

    /** minX, maxX, minZ, maxZ covered by a template of size (sx, sz) placed at origin with a rotation. */
    private static int[] coveredBox(Offset o, int sx, int sz, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new int[]{o.x(), o.x() + sx - 1, o.z(), o.z() + sz - 1};
            case CLOCKWISE_90 -> new int[]{o.x() - (sz - 1), o.x(), o.z(), o.z() + sx - 1};
            case COUNTERCLOCKWISE_90 -> new int[]{o.x(), o.x() + sz - 1, o.z() - (sx - 1), o.z()};
            case CLOCKWISE_180 -> new int[]{o.x() - (sx - 1), o.x(), o.z() - (sz - 1), o.z()};
        };
    }

    /** minX, maxX, minZ, maxZ of the 8x8 rooms of a room's units. */
    private static int[] unitBox(Room room) {
        int[] box = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (Cell cell : room.cells()) {
            Offset node = BuildPlanner.node(cell);
            box[0] = Math.min(box[0], node.x() - 4);
            box[1] = Math.max(box[1], node.x() + 3);
            box[2] = Math.min(box[2], node.z() - 4);
            box[3] = Math.max(box[3], node.z() + 3);
        }
        return box;
    }

    private static int indexOf(List<BuildStep> steps, Predicate<BuildStep> test) {
        for (int i = 0; i < steps.size(); i++) {
            if (test.test(steps.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
