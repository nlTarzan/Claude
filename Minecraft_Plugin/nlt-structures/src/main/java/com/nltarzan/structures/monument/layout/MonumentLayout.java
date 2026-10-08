package com.nltarzan.structures.monument.layout;

import com.nltarzan.structures.monument.layout.MonumentGrid.Wall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * A complete monument layout: which walls are open and which room goes where. Contains no world
 * blocks, so it can be generated and tested without a server. The same seed and settings always
 * give the same layout.
 */
public final class MonumentLayout {

    private final long seed;
    private final double newestChance;
    private final MonumentGrid grid;
    private final List<Room> rooms;
    private final Room[] roomByCell;

    private MonumentLayout(long seed, double newestChance, MonumentGrid grid, List<Room> rooms) {
        this.seed = seed;
        this.newestChance = newestChance;
        this.grid = grid;
        this.rooms = rooms;
        this.roomByCell = new Room[grid.cells().size()];
        for (Room room : rooms) {
            for (Cell cell : room.cells()) {
                roomByCell[cell.id()] = room;
            }
        }
    }

    public static MonumentLayout generate(long seed, MazeSettings settings) {
        // SplittableRandom mixes the seed well; java.util.Random gives near-identical first values for seeds 0, 1, 2...
        RandomGenerator random = new SplittableRandom(seed);
        double newestChance = settings.drawNewestChance(random);

        // Like the datapack: each wing is wing_open or wing_closed, 50/50.
        MonumentGrid grid = new MonumentGrid(random.nextBoolean(), random.nextBoolean());
        // Like vanilla, the core goes in the back two rows of the bottom two floors, at a random x.
        Cell coreAnchor = grid.at(random.nextInt(MonumentGrid.WIDTH - 1), 0, MonumentGrid.ROWS - 2);

        GrowingTreeGenerator.carve(grid, random, newestChance);
        List<Room> rooms = RoomFitter.fit(grid, coreAnchor, Set.of(), random);

        // Too few sponge rooms: rewire units into floor-only spots and fit the rooms again.
        Set<Cell> excluded = new HashSet<>(RoomFitter.coreCells(grid, coreAnchor));
        excluded.add(grid.entry());
        Set<Cell> sponges = new LinkedHashSet<>();
        while (count(rooms, RoomType.SPONGE) < settings.minSpongeRooms()) {
            Cell spot = SpongeRooms.makeSpongeSpot(grid, excluded, sponges, random);
            if (spot == null) {
                break;
            }
            sponges.add(spot);
            rooms = RoomFitter.fit(grid, coreAnchor, sponges, random);
        }
        return new MonumentLayout(seed, newestChance, grid, rooms);
    }

    private static long count(List<Room> rooms, RoomType type) {
        return rooms.stream().filter(r -> r.type() == type).count();
    }

    public long seed() {
        return seed;
    }

    /** This layout's chance of continuing from the newest unit, see {@link GrowingTreeGenerator}. */
    public double newestChance() {
        return newestChance;
    }

    public MonumentGrid grid() {
        return grid;
    }

    public List<Room> rooms() {
        return rooms;
    }

    /** The room a unit belongs to, or {@code null} for the dummy units. */
    public Room roomOf(Cell cell) {
        return roomByCell[cell.id()];
    }

    /**
     * Open walls that need a doorway (horizontal) or a floor/ceiling opening (vertical) when
     * building: every open wall between two different rooms, including the wing and penthouse links.
     */
    public List<Wall> openings() {
        List<Wall> openings = new ArrayList<>();
        for (Wall wall : grid.walls()) {
            if (wall.cell().isOpen(wall.direction()) && roomOf(wall.cell()) != roomOf(wall.other())) {
                openings.add(wall);
            }
        }
        return openings;
    }
}
