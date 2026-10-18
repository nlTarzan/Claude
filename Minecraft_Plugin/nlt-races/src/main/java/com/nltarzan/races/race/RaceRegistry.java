package com.nltarzan.races.race;

import com.nltarzan.races.util.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** All races, each stored in plugins/nlT-Races/races/&lt;name&gt;.yml. */
public final class RaceRegistry {

    /** No underscores: placeholders use _ as a separator (%nltraces_<race>_week_1%). */
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9-]{1,32}");

    public record ButtonLink(Race race, ButtonAction action) {
    }

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<String, Race> races = new TreeMap<>();

    public RaceRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "races");
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public void load() {
        races.clear();
        File[] files = folder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            if (!isValidName(name)) {
                plugin.getLogger().warning("Skipping races/" + file.getName()
                        + ": race names may only use a-z, 0-9 and -.");
                continue;
            }
            try {
                races.put(name, read(name, YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Skipping races/" + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void save(Race race) {
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create " + folder);
            return;
        }
        try {
            write(race).save(fileOf(race.getName()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save race " + race.getName(), e);
        }
    }

    public Race create(String name, RaceFormat format, TravelMode travel, VehicleType vehicle) {
        Race race = new Race(name, format, travel);
        race.setVehicle(vehicle);
        races.put(name, race);
        save(race);
        return race;
    }

    public boolean delete(String name) {
        Race race = races.remove(name);
        if (race == null) {
            return false;
        }
        File file = fileOf(name);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete " + file);
        }
        return true;
    }

    public Race get(String name) {
        return races.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Race> all() {
        return Collections.unmodifiableCollection(races.values());
    }

    public List<String> names() {
        return List.copyOf(races.keySet());
    }

    /** The race and action a block is linked to, or null. */
    public ButtonLink findButton(BlockPos pos) {
        for (Race race : races.values()) {
            ButtonAction action = race.getButtons().get(pos);
            if (action != null) {
                return new ButtonLink(race, action);
            }
        }
        return null;
    }

    private File fileOf(String name) {
        return new File(folder, name + ".yml");
    }

    // ---------------------------------------------------------------- yml format

    private static YamlConfiguration write(Race race) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().setHeader(List.of(
                "nlT-Races race file. After editing, run /nltraces reload.",
                "format: SOLO, GROUP or POINTS | travel: FOOT, ELYTRA, VEHICLE or FREE | rules: STRICT or LENIENT",
                "boat-type (boat races): OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, PALE_OAK, BAMBOO",
                "display-name: shown to players instead of the ID (spaces allowed); leave it out to show the ID",
                "checkpoint-particles: true or false; leave it out for automatic (on when there are middle checkpoints)",
                "Regions are world,minX,minY,minZ,maxX,maxY,maxZ. Buttons are world,x,y,z. Settings are in seconds."));
        yml.set("display-name", race.getCustomName());
        yml.set("checkpoint-particles", race.getCheckpointParticlesSetting());
        yml.set("format", race.getFormat().name());
        yml.set("travel", race.getTravel().name());
        yml.set("vehicle", race.getVehicle() == null ? null : race.getVehicle().name());
        yml.set("boat-type", race.getVehicle() == VehicleType.BOAT ? race.getBoatWood().name() : null);
        yml.set("rules", race.getRules().name());

        yml.set("lobby", race.getLobby());
        yml.set("solo-start", race.getSoloStart());
        yml.set("grid.max", race.getGridMax());
        yml.set("grid.region", race.getGridRegion() == null ? null : race.getGridRegion().serialize());
        yml.set("grid.slots", new ArrayList<>(race.getGridSlots()));
        for (int place = 1; place <= 3; place++) {
            yml.set("podium." + place, race.getPodium(place));
        }

        yml.set("checkpoints.start", race.getStartCheckpoint() == null ? null : race.getStartCheckpoint().serialize());
        yml.set("checkpoints.middle", race.getCheckpoints().stream().map(Region::serialize).toList());
        yml.set("checkpoints.finish",
                race.getFinishCheckpoint() == null ? null : race.getFinishCheckpoint().serialize());

        List<Map<String, Object>> zones = new ArrayList<>();
        for (PointZone zone : race.getPointZones()) {
            Location center = zone.center();
            Map<String, Object> map = new LinkedHashMap<>(); // a position only: zones have no facing
            map.put("world", center.getWorld().getName());
            map.put("x", center.getX());
            map.put("y", center.getY());
            map.put("z", center.getZ());
            map.put("radius", zone.radius());
            zones.add(map);
        }
        yml.set("point-zones", zones);

        for (ButtonAction action : ButtonAction.values()) {
            List<String> blocks = race.getButtons().entrySet().stream()
                    .filter(entry -> entry.getValue() == action)
                    .map(entry -> entry.getKey().serialize())
                    .toList();
            yml.set("buttons." + action.name().toLowerCase(Locale.ROOT), blocks);
        }

        for (RaceSetting setting : RaceSetting.values()) {
            yml.set("settings." + setting.key(), race.getSetting(setting));
        }
        return yml;
    }

    private static Race read(String name, YamlConfiguration yml) {
        RaceFormat format = require(Enums.parse(RaceFormat.class, yml.getString("format")), "format");
        TravelMode travel = require(Enums.parse(TravelMode.class, yml.getString("travel")), "travel");
        Race race = new Race(name, format, travel);
        race.setCustomName(yml.getString("display-name"));
        race.setCheckpointParticles(yml.isBoolean("checkpoint-particles") ? yml.getBoolean("checkpoint-particles") : null);
        if (travel == TravelMode.VEHICLE) {
            race.setVehicle(Enums.parse(VehicleType.class, yml.getString("vehicle")));
            BoatWood wood = Enums.parse(BoatWood.class, yml.getString("boat-type"));
            race.setBoatWood(wood == null ? BoatWood.OAK : wood);
        }
        Rules rules = Enums.parse(Rules.class, yml.getString("rules"));
        if (rules == null && "LOOSE".equalsIgnoreCase(yml.getString("strictness"))) {
            rules = Rules.LENIENT; // files saved before "strictness: HARD/LOOSE" was renamed
        }
        race.setRules(rules == null ? Rules.STRICT : rules);

        race.setLobby(yml.getLocation("lobby"));
        race.setSoloStart(yml.getLocation("solo-start"));
        race.setGridMax(Math.max(1, yml.getInt("grid.max", Race.DEFAULT_GRID_MAX)));
        if (yml.isString("grid.region")) {
            race.setGridRegion(Region.parse(yml.getString("grid.region")));
        }
        for (Object slot : yml.getList("grid.slots", List.of())) {
            if (slot instanceof Location location) {
                race.getGridSlots().add(location);
            }
        }
        for (int place = 1; place <= 3; place++) {
            race.setPodium(place, yml.getLocation("podium." + place));
        }

        if (yml.isString("checkpoints.start")) {
            race.setStartCheckpoint(Region.parse(yml.getString("checkpoints.start")));
        }
        for (String raw : yml.getStringList("checkpoints.middle")) {
            race.getCheckpoints().add(Region.parse(raw));
        }
        if (yml.isString("checkpoints.finish")) {
            race.setFinishCheckpoint(Region.parse(yml.getString("checkpoints.finish")));
        }

        for (Map<?, ?> zone : yml.getMapList("point-zones")) {
            if (!(zone.get("radius") instanceof Number radius)) {
                continue;
            }
            Location center = null;
            if (zone.get("world") instanceof String worldName && zone.get("x") instanceof Number x
                    && zone.get("y") instanceof Number y && zone.get("z") instanceof Number z) {
                World world = Bukkit.getWorld(worldName);
                center = world == null ? null : new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
            } else if (zone.get("location") instanceof Location location) {
                center = location; // files saved before zones dropped their facing
            }
            if (center != null) {
                race.getPointZones().add(new PointZone(center, radius.doubleValue()));
            }
        }

        for (ButtonAction action : ButtonAction.values()) {
            for (String raw : yml.getStringList("buttons." + action.name().toLowerCase(Locale.ROOT))) {
                race.getButtons().put(BlockPos.parse(raw), action);
            }
        }

        for (RaceSetting setting : RaceSetting.values()) {
            if (yml.contains("settings." + setting.key())) {
                race.setSetting(setting, yml.getInt("settings." + setting.key()));
            }
        }
        return race;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("missing or invalid '" + field + "'");
        }
        return value;
    }
}
