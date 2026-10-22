package com.nltarzan.structures.monument.build;

import com.nltarzan.structures.monument.build.BuildStep.Block;
import com.nltarzan.structures.monument.build.BuildStep.Fill;
import com.nltarzan.structures.monument.build.BuildStep.Offset;
import com.nltarzan.structures.monument.build.BuildStep.Place;
import com.nltarzan.structures.monument.build.BuildStep.Rotation;
import com.nltarzan.structures.monument.build.BuildStep.SpawnElder;
import com.nltarzan.structures.monument.layout.Cell;
import com.nltarzan.structures.monument.layout.GridDirection;
import com.nltarzan.structures.monument.layout.MonumentGrid.Wall;
import com.nltarzan.structures.monument.layout.MonumentLayout;
import com.nltarzan.structures.monument.layout.Room;
import com.nltarzan.structures.monument.layout.RoomType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Turns a layout into build steps, following the datapack's maze functions (reset, end, place/*):
 * the empty monument, entrance, wings and penthouse, the floors, every room, the core's corner
 * walls, sponges and pillars, the openings between rooms, and the three Elder Guardians.
 *
 * <p>Unit positions are the datapack's node markers: {@code ~(8x - 16) ~(4 floor) ~(3 - 8 row)}
 * from the center. A unit's 8x8 room runs from 4 blocks below its node to 3 above, on x and z.
 */
public final class BuildPlanner {

    /** Mixed into the layout seed, so the build's own random choices differ from the layout's. */
    private static final long BUILD_SALT = 0x5EED_B11DL;

    private BuildPlanner() {
    }

    public static Offset node(Cell cell) {
        return new Offset(8 * cell.x() - 16, 4 * cell.floor(), 3 - 8 * cell.row());
    }

    public static List<BuildStep> plan(MonumentLayout layout) {
        RandomGenerator random = new SplittableRandom(layout.seed() ^ BUILD_SALT);
        List<BuildStep> steps = new ArrayList<>();

        // reset.mcfunction: the empty monument, entrance, wings and top room.
        steps.add(new Place("monument_empty_nw", new Offset(-29, -1, -29), Rotation.NONE));
        steps.add(new Place("monument_empty_ne", new Offset(0, -1, -29), Rotation.NONE));
        steps.add(new Place("monument_empty_sw", new Offset(-29, -1, 0), Rotation.NONE));
        steps.add(new Place("monument_empty_se", new Offset(0, -1, 0), Rotation.NONE));
        Room entry = layout.roomOf(layout.grid().entry());
        steps.add(new Place(RoomType.ENTRY.template(), footprintMin(entry), Rotation.NONE));
        steps.add(new Place(wing(layout.grid().isRightWingOpen()), new Offset(9, 0, 6), Rotation.NONE));
        steps.add(new Place(wing(layout.grid().isLeftWingOpen()), new Offset(-24, 0, 6), Rotation.NONE));
        steps.add(new Place("room/penthouse", new Offset(-8, 11, -8), Rotation.NONE));

        // end.mcfunction: the floors between the levels.
        steps.add(new Fill(new Offset(-22, 3, -25), new Offset(21, 3, 6), Block.PRISMARINE));
        steps.add(new Fill(new Offset(-15, 7, -15), new Offset(14, 7, 6), Block.PRISMARINE));

        // The rooms, core first.
        List<Room> rooms = new ArrayList<>(layout.rooms());
        rooms.remove(entry);
        rooms.sort(Comparator.comparing((Room r) -> r.type() != RoomType.CORE));
        for (Room room : rooms) {
            steps.add(placeRoom(room, random));
        }

        for (Room room : rooms) {
            switch (room.type()) {
                case CORE -> coreCornerWalls(room, steps);
                case SPONGE -> sponges(node(room.anchor()), random, steps);
                case SMALL_PILLARS, SMALL_DARK -> {
                    if (room.centerPillar()) {
                        centerPillar(node(room.anchor()), steps);
                    }
                }
                default -> {
                }
            }
        }

        // Openings: floor/ceiling openings first, then the doorways (place/horizontal_connecters).
        List<Wall> openings = layout.openings();
        for (Wall wall : openings) {
            if (wall.direction() == GridDirection.UP) {
                Offset upper = node(wall.other());
                steps.add(new Place("roof_open", upper.plus(-4, -1, -4), Rotation.NONE));
            }
        }
        for (Wall wall : openings) {
            if (wall.direction() != GridDirection.UP) {
                steps.add(doorway(node(wall.cell()), node(wall.other())));
            }
        }

        // The three Elder Guardians: the top room and both wings.
        steps.add(new SpawnElder(0, 12, 0));
        steps.add(new SpawnElder(16, 4, 11));
        steps.add(new SpawnElder(-16, 4, 11));
        return steps;
    }

    private static String wing(boolean open) {
        return open ? "room/wing_open" : "room/wing_closed";
    }

    /** The lowest corner of the blocks a room's units take up. */
    static Offset footprintMin(Room room) {
        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int z = Integer.MAX_VALUE;
        for (Cell cell : room.cells()) {
            Offset node = node(cell);
            x = Math.min(x, node.x() - 4);
            y = Math.min(y, node.y());
            z = Math.min(z, node.z() - 4);
        }
        return new Offset(x, y, z);
    }

    /**
     * Places a room so the template exactly covers its units. The 2-wide templates are saved running
     * along X; along X they're placed as is or turned 180, along Z turned 90 either way, picked at
     * random so the one-sided designs (like the U-shaped hall) face both ways, as in the datapack.
     */
    static Place placeRoom(Room room, RandomGenerator random) {
        Offset min = footprintMin(room);
        if (room.axis() == null) {
            return new Place(room.type().template(), min, Rotation.NONE);
        }
        // Footprint size: 16 along the room's axis, 8 across it.
        int sizeX = room.axis() == GridDirection.EAST ? 16 : 8;
        int sizeZ = room.axis() == GridDirection.EAST ? 8 : 16;
        Rotation rotation = room.axis() == GridDirection.EAST
                ? (random.nextBoolean() ? Rotation.NONE : Rotation.CLOCKWISE_180)
                : (random.nextBoolean() ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
        return new Place(room.type().template(), rotatedOrigin(min, sizeX, sizeZ, rotation), rotation);
    }

    /**
     * Where to put a template's origin so that, after rotating around it, the template covers the
     * box starting at {@code min} with the given (already rotated) size.
     */
    static Offset rotatedOrigin(Offset min, int sizeX, int sizeZ, Rotation rotation) {
        return switch (rotation) {
            case NONE -> min;
            case CLOCKWISE_90 -> min.plus(sizeX - 1, 0, 0);
            case COUNTERCLOCKWISE_90 -> min.plus(0, 0, sizeZ - 1);
            case CLOCKWISE_180 -> min.plus(sizeX - 1, 0, sizeZ - 1);
        };
    }

    /**
     * place/core.mcfunction: a core against the left or right side opens its bottom floor into the
     * passage between the wings. Offsets are from the core's back-left unit, like the datapack.
     */
    private static void coreCornerWalls(Room core, List<BuildStep> steps) {
        Cell backLeft = core.cells().stream()
                .filter(c -> c.floor() == 0)
                .min(Comparator.comparingInt(Cell::x).thenComparing(Comparator.comparingInt(Cell::row).reversed()))
                .orElseThrow();
        Offset a = node(backLeft);
        if (backLeft.x() == 0) {
            steps.add(new Fill(a.plus(-4, 0, -4), a.plus(-4, 2, 8), Block.WATER));
            steps.add(new Fill(a.plus(-4, 0, -4), a.plus(8, 2, -4), Block.WATER));
        }
        if (backLeft.x() == 3) {
            steps.add(new Fill(a.plus(11, 0, -4), a.plus(11, 2, 8), Block.WATER));
            steps.add(new Fill(a.plus(-1, 0, -4), a.plus(11, 2, -4), Block.WATER));
        }
    }

    /**
     * place/treasure.mcfunction, with vanilla's chances: each of the 36 columns is empty (1/3), has
     * one sponge at the top (1/2) or two (1/6). The template's own 4 sponges are cleared first.
     */
    private static void sponges(Offset node, RandomGenerator random, List<BuildStep> steps) {
        steps.add(new Fill(node.plus(-3, 1, -3), node.plus(2, 2, 2), Block.WATER));
        for (int dx = -3; dx <= 2; dx++) {
            for (int dz = -3; dz <= 2; dz++) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                int bottom = random.nextInt(4) == 0 ? 1 : 2;
                steps.add(new Fill(node.plus(dx, bottom, dz), node.plus(dx, 2, dz), Block.WET_SPONGE));
            }
        }
    }

    /** Vanilla's 2x2 center pillar: prismarine bricks at the bottom and top, prismarine in between. */
    private static void centerPillar(Offset node, List<BuildStep> steps) {
        steps.add(new Fill(node.plus(-1, 0, -1), node.plus(0, 0, 0), Block.PRISMARINE_BRICKS));
        steps.add(new Fill(node.plus(-1, 1, -1), node.plus(0, 1, 0), Block.PRISMARINE));
        steps.add(new Fill(node.plus(-1, 2, -1), node.plus(0, 2, 0), Block.PRISMARINE_BRICKS));
    }

    /**
     * place/horizontal_connecters.mcfunction: a 2 wide, 2 high water doorway through both walls
     * between two neighboring units.
     */
    static Fill doorway(Offset a, Offset b) {
        if (a.x() != b.x()) {
            int low = Math.min(a.x(), b.x());
            int high = Math.max(a.x(), b.x());
            return new Fill(new Offset(low + 2, a.y(), a.z() - 1), new Offset(high - 3, a.y() + 1, a.z()), Block.WATER);
        }
        int low = Math.min(a.z(), b.z());
        int high = Math.max(a.z(), b.z());
        return new Fill(new Offset(a.x() - 1, a.y(), low + 2), new Offset(a.x(), a.y() + 1, high - 3), Block.WATER);
    }
}
