package com.nltarzan.races.race;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A race course: its format, travel rules, locations, checkpoints and linked buttons. */
public final class Race {

    public static final int DEFAULT_GRID_MAX = 8;
    /** Points races pick the next zone at random, so they need at least two to choose from. */
    public static final int MIN_POINT_ZONES = 2;

    private final String name;
    private RaceFormat format;
    private TravelMode travel;
    private VehicleType vehicle;
    /** Boat races: which wood the boats are made of. */
    private BoatWood boatWood = BoatWood.OAK;
    private Rules rules = Rules.STRICT;

    private Location lobby;
    private Location soloStart;
    private Region gridRegion;
    private final List<Location> gridSlots = new ArrayList<>();
    private int gridMax = DEFAULT_GRID_MAX;
    private final Location[] podium = new Location[3];

    private Region startCheckpoint;
    private final List<Region> checkpoints = new ArrayList<>();
    private Region finishCheckpoint;
    private final List<PointZone> pointZones = new ArrayList<>();

    private final Map<BlockPos, ButtonAction> buttons = new LinkedHashMap<>();
    private final Map<RaceSetting, Integer> settings = new EnumMap<>(RaceSetting.class);

    public Race(String name, RaceFormat format, TravelMode travel) {
        this.name = name;
        this.format = format;
        this.travel = travel;
    }

    /**
     * What still has to be set up before the race can be used, as keys under
     * {@code info.missing} in messages.yml. Empty when the race is ready.
     */
    public List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (lobby == null) {
            missing.add("lobby");
        }
        if (travel == TravelMode.VEHICLE && vehicle == null) {
            missing.add("vehicle");
        }
        if (format == RaceFormat.SOLO) {
            if (soloStart == null) {
                missing.add("solo-start");
            } else if (startCheckpoint != null && startCheckpoint.contains(soloStart)) {
                // The timer starts on entering the start checkpoint, so racers must begin outside it.
                missing.add("solo-start-inside");
            }
        } else {
            if (gridRegion == null && gridSlots.isEmpty()) {
                missing.add("grid");
            }
            for (int place = 1; place <= 3; place++) {
                if (podium[place - 1] == null) {
                    missing.add("podium-" + place);
                }
            }
        }
        if (format == RaceFormat.POINTS) {
            if (pointZones.size() < MIN_POINT_ZONES) {
                missing.add("point-zones");
            }
        } else {
            if (startCheckpoint == null) {
                missing.add("start-checkpoint");
            }
            if (finishCheckpoint == null) {
                missing.add("finish-checkpoint");
            }
        }
        for (ButtonAction action : List.of(ButtonAction.PREP, ButtonAction.START, ButtonAction.JOIN)) {
            if (action.appliesTo(format) && !buttons.containsValue(action)) {
                missing.add("button-" + action.name().toLowerCase(Locale.ROOT));
            }
        }
        return missing;
    }

    public boolean isReady() {
        return missing().isEmpty();
    }

    public int getSetting(RaceSetting setting) {
        return settings.getOrDefault(setting, setting.defaultValue());
    }

    public void setSetting(RaceSetting setting, int value) {
        settings.put(setting, setting.clamp(value));
    }

    private static Location copy(Location location) {
        return location == null ? null : location.clone();
    }

    public String getName() {
        return name;
    }

    public RaceFormat getFormat() {
        return format;
    }

    public void setFormat(RaceFormat format) {
        this.format = format;
    }

    public TravelMode getTravel() {
        return travel;
    }

    public void setTravel(TravelMode travel) {
        this.travel = travel;
        if (travel != TravelMode.VEHICLE) {
            vehicle = null;
        }
    }

    public VehicleType getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public BoatWood getBoatWood() {
        return boatWood;
    }

    public void setBoatWood(BoatWood boatWood) {
        this.boatWood = boatWood;
    }

    /** The entity id to spawn for this race's vehicle; boats use the race's wood type. */
    public String vehicleEntityId() {
        return vehicle == VehicleType.BOAT ? boatWood.entityId() : vehicle.entityId();
    }

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public Location getLobby() {
        return copy(lobby);
    }

    public void setLobby(Location lobby) {
        this.lobby = copy(lobby);
    }

    public Location getSoloStart() {
        return copy(soloStart);
    }

    public void setSoloStart(Location soloStart) {
        this.soloStart = copy(soloStart);
    }

    public Region getGridRegion() {
        return gridRegion;
    }

    public void setGridRegion(Region gridRegion) {
        this.gridRegion = gridRegion;
    }

    /** Live list of manual grid slots; locations include the direction racers face. */
    public List<Location> getGridSlots() {
        return gridSlots;
    }

    public int getGridMax() {
        return gridMax;
    }

    public void setGridMax(int gridMax) {
        this.gridMax = gridMax;
    }

    /** @param place 1, 2 or 3 */
    public Location getPodium(int place) {
        return copy(podium[place - 1]);
    }

    public void setPodium(int place, Location location) {
        podium[place - 1] = copy(location);
    }

    public Region getStartCheckpoint() {
        return startCheckpoint;
    }

    public void setStartCheckpoint(Region startCheckpoint) {
        this.startCheckpoint = startCheckpoint;
    }

    /** Live list of the middle checkpoints, in the order they must be passed. */
    public List<Region> getCheckpoints() {
        return checkpoints;
    }

    public Region getFinishCheckpoint() {
        return finishCheckpoint;
    }

    public void setFinishCheckpoint(Region finishCheckpoint) {
        this.finishCheckpoint = finishCheckpoint;
    }

    /** Live list of point zones (points races). */
    public List<PointZone> getPointZones() {
        return pointZones;
    }

    /** Live map of linked button blocks. */
    public Map<BlockPos, ButtonAction> getButtons() {
        return buttons;
    }
}
