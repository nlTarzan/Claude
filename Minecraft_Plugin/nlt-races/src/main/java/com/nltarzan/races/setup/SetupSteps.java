package com.nltarzan.races.setup;

import com.nltarzan.races.race.BlockPos;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.PointZone;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.Region;
import com.nltarzan.races.setup.SetupStep.Kind;
import com.nltarzan.races.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The steps each kind of race needs, and the individual steps used by /nltraces set. */
public final class SetupSteps {

    public static final double DEFAULT_ZONE_RADIUS = 3.0;
    static final String GRID_SLOT_ID = "grid-slot";
    static final String POINT_ZONE_ID = "point-zone";

    /** Grid spots and point zones are sets of spots: shown as markers, rebuilt in the guide, undoable. */
    static boolean isSetOfSpots(SetupStep step) {
        return step.id().equals(GRID_SLOT_ID) || step.id().equals(POINT_ZONE_ID);
    }

    /** The saved spots of a grid-spot or point-zone step, as the blocks they stand on (for markers). */
    static List<Location> spotBlocks(SetupStep step, Race race) {
        List<Location> spots = step.id().equals(GRID_SLOT_ID) ? race.getGridSlots()
                : race.getPointZones().stream().map(PointZone::center).toList();
        return spots.stream()
                .map(spot -> new Location(spot.getWorld(), spot.getBlockX(), spot.getBlockY() - 1, spot.getBlockZ()))
                .toList();
    }

    /** How many spots a grid-spot or point-zone step has saved (a grid area counts as one). */
    static int spotCount(SetupStep step, Race race) {
        if (step.id().equals(GRID_SLOT_ID)) {
            return race.getGridSlots().size() + (race.getGridRegion() != null ? 1 : 0);
        }
        return race.getPointZones().size();
    }

    /** Removes every saved spot of a grid-spot or point-zone step. */
    static void clearSpots(SetupStep step, Race race) {
        if (step.id().equals(GRID_SLOT_ID)) {
            race.getGridSlots().clear();
            race.setGridRegion(null);
        } else {
            race.getPointZones().clear();
        }
    }

    /** Removes the most recently added spot of a grid-spot or point-zone step. */
    static void removeLastSpot(SetupStep step, Race race) {
        List<?> spots = step.id().equals(GRID_SLOT_ID) ? race.getGridSlots() : race.getPointZones();
        if (!spots.isEmpty()) {
            spots.removeLast();
        }
    }

    private SetupSteps() {
    }

    /** The guided setup for a race, in order. Always starts with the lobby and ends with the lobby button. */
    public static List<SetupStep> forRace(Race race) {
        List<SetupStep> steps = new ArrayList<>();
        steps.add(lobby());
        switch (race.getFormat()) {
            case SOLO -> {
                steps.add(button(ButtonAction.START));
                steps.add(soloStart());
                steps.add(startCheckpoint());
                steps.add(middleCheckpoint(true));
                steps.add(finishCheckpoint());
            }
            case GROUP -> {
                addPodiumsAndGroupButtons(steps);
                steps.add(gridSlot(true));
                steps.add(startCheckpoint());
                steps.add(middleCheckpoint(true));
                steps.add(finishCheckpoint());
            }
            case POINTS -> {
                addPodiumsAndGroupButtons(steps);
                steps.add(gridSlot(true));
                steps.add(pointZone(true, DEFAULT_ZONE_RADIUS));
            }
        }
        steps.add(button(ButtonAction.LOBBY));
        return steps;
    }

    private static void addPodiumsAndGroupButtons(List<SetupStep> steps) {
        for (int place = 1; place <= 3; place++) {
            steps.add(podium(place));
        }
        steps.add(button(ButtonAction.PREP));
        steps.add(button(ButtonAction.JOIN));
        steps.add(button(ButtonAction.START));
    }

    // ---------------------------------------------------------------- buttons

    public static SetupStep button(ButtonAction action) {
        return new SetupStep("button-" + action.name().toLowerCase(Locale.ROOT), Kind.BUTTON, false,
                race -> race.getButtons().containsValue(action),
                race -> {
                    String linked = race.getButtons().entrySet().stream()
                            .filter(entry -> entry.getValue() == action)
                            .map(entry -> entry.getKey().describe())
                            .collect(Collectors.joining(" | "));
                    return linked.isEmpty() ? null : linked;
                },
                (race, value) -> race.getButtons().put(BlockPos.of((Block) value), action));
    }

    // ---------------------------------------------------------------- single locations

    public static SetupStep lobby() {
        return location("lobby", Race::getLobby, Race::setLobby);
    }

    public static SetupStep soloStart() {
        return location("solo-start", Race::getSoloStart, Race::setSoloStart);
    }

    public static SetupStep podium(int place) {
        return location("podium-" + place, race -> race.getPodium(place),
                (race, location) -> race.setPodium(place, location));
    }

    private static SetupStep location(String id, Function<Race, Location> getter,
                                      BiConsumer<Race, Location> setter) {
        return new SetupStep(id, Kind.LOCATION, false,
                race -> getter.apply(race) != null,
                race -> getter.apply(race) == null ? null : Locations.describe(getter.apply(race)),
                (race, value) -> setter.accept(race, (Location) value));
    }

    // ---------------------------------------------------------------- checkpoints

    public static SetupStep startCheckpoint() {
        return region("start-checkpoint", Race::getStartCheckpoint, Race::setStartCheckpoint);
    }

    public static SetupStep finishCheckpoint() {
        return region("finish-checkpoint", Race::getFinishCheckpoint, Race::setFinishCheckpoint);
    }

    private static SetupStep region(String id, Function<Race, Region> getter, BiConsumer<Race, Region> setter) {
        return new SetupStep(id, Kind.REGION, false,
                race -> getter.apply(race) != null,
                race -> getter.apply(race) == null ? null : getter.apply(race).describe(),
                (race, value) -> setter.accept(race, (Region) value));
    }

    /**
     * Adds a middle checkpoint. In the guided setup it repeats until "next" and counts as optional,
     * since a race doesn't need middle checkpoints.
     */
    public static SetupStep middleCheckpoint(boolean repeating) {
        return new SetupStep("middle-checkpoint", Kind.REGION, repeating,
                race -> true,
                race -> race.getCheckpoints().isEmpty() ? null : race.getCheckpoints().size() + " checkpoint(s)",
                (race, value) -> race.getCheckpoints().add((Region) value));
    }

    // ---------------------------------------------------------------- grid

    public static SetupStep gridRegion() {
        return new SetupStep("grid-region", Kind.REGION, false,
                race -> race.getGridRegion() != null || !race.getGridSlots().isEmpty(),
                race -> race.getGridRegion() != null ? race.getGridRegion().describe()
                        : race.getGridSlots().isEmpty() ? null : race.getGridSlots().size() + " spot(s)",
                (race, value) -> {
                    race.getGridSlots().clear(); // a race uses either a region or spots
                    race.setGridRegion((Region) value);
                });
    }

    /**
     * Adds one starting spot. In the guided setup it repeats until "next"; the number of spots is the
     * maximum number of players (unless lowered with /nltraces set &lt;race&gt; grid max).
     */
    public static SetupStep gridSlot(boolean repeating) {
        return new SetupStep(GRID_SLOT_ID, Kind.LOCATION, repeating,
                race -> race.getGridRegion() != null || !race.getGridSlots().isEmpty(),
                race -> race.getGridSlots().isEmpty() ? null : race.getGridSlots().size() + " spot(s)",
                (race, value) -> {
                    race.setGridRegion(null); // a race uses either a region or spots
                    race.getGridSlots().add((Location) value);
                });
    }

    // ---------------------------------------------------------------- point zones

    /** Points races need at least {@link Race#MIN_POINT_ZONES}; in the guided setup it repeats until "next". */
    public static SetupStep pointZone(boolean repeating, double radius) {
        return new SetupStep(POINT_ZONE_ID, Kind.LOCATION, repeating,
                race -> race.getPointZones().size() >= Race.MIN_POINT_ZONES,
                race -> race.getPointZones().isEmpty() ? null : race.getPointZones().size() + " zone(s)",
                (race, value) -> race.getPointZones().add(new PointZone((Location) value, radius)));
    }
}
