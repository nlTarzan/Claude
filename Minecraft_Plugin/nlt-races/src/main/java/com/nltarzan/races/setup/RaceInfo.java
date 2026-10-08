package com.nltarzan.races.setup;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.race.BoatWood;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.PointZone;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.RaceSetting;
import com.nltarzan.races.race.Region;
import com.nltarzan.races.race.VehicleType;
import com.nltarzan.races.util.Enums;
import com.nltarzan.races.util.Locations;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The overview shown by /nltraces info and at the start and end of a guided setup. */
public final class RaceInfo {

    private RaceInfo() {
    }

    public static void show(NltRacesPlugin plugin, Audience to, Race race, String label) {
        Messages msg = plugin.getMessages();
        msg.send(to, "info.header", "race", race.getName(), "format", Enums.display(race.getFormat()),
                "travel", travelText(race), "rules", Enums.display(race.getRules()));
        line(msg, to, "Lobby", race.getLobby());

        if (race.getFormat() == RaceFormat.SOLO) {
            line(msg, to, "Start", race.getSoloStart());
        } else {
            Region gridRegion = race.getGridRegion();
            if (gridRegion != null) {
                line(msg, to, "Grid region", gridRegion.describe());
            } else {
                line(msg, to, "Grid spots", race.getGridSlots().isEmpty() ? null
                        : race.getGridSlots().size() + " spot(s)");
            }
            line(msg, to, "Max players", String.valueOf(race.getGridMax()));
            for (int place = 1; place <= 3; place++) {
                line(msg, to, "Podium " + place, race.getPodium(place));
            }
        }

        if (race.getFormat() == RaceFormat.POINTS) {
            List<PointZone> zones = race.getPointZones();
            line(msg, to, "Point zones", zones.isEmpty() ? null : String.valueOf(zones.size()));
            for (int i = 0; i < zones.size(); i++) {
                line(msg, to, "  #" + (i + 1), Locations.describe(zones.get(i).center())
                        + " (radius " + zones.get(i).radius() + ")");
            }
        } else {
            line(msg, to, "Start checkpoint", describe(race.getStartCheckpoint()));
            List<Region> middle = race.getCheckpoints();
            for (int i = 0; i < middle.size(); i++) {
                line(msg, to, "Checkpoint " + (i + 1), middle.get(i).describe());
            }
            line(msg, to, "Finish checkpoint", describe(race.getFinishCheckpoint()));
        }

        Map<ButtonAction, List<String>> buttons = race.getButtons().entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(entry -> entry.getKey().describe(), Collectors.toList())));
        for (ButtonAction action : ButtonAction.values()) {
            boolean expected = action.appliesTo(race.getFormat()) && action != ButtonAction.GATE; // gates are optional
            if (expected || buttons.containsKey(action)) {
                List<String> blocks = buttons.get(action);
                line(msg, to, "Button " + Enums.display(action), blocks == null ? null : String.join(" | ", blocks));
            }
        }

        for (RaceSetting setting : RaceSetting.values()) {
            if (setting.appliesTo(race)) {
                line(msg, to, setting.key(), String.valueOf(race.getSetting(setting)));
            }
        }
        showStatus(plugin, to, race, label);
    }

    /** "Ready to use", or the list of what's still missing with the command for each. */
    public static void showStatus(NltRacesPlugin plugin, Audience to, Race race, String label) {
        Messages msg = plugin.getMessages();
        List<String> missing = race.missing();
        if (missing.isEmpty()) {
            msg.send(to, "info.ready");
            return;
        }
        msg.send(to, "info.missing-header", "race", race.getName(), "label", label);
        for (String key : missing) {
            msg.sendRaw(to, "info.missing-line", "item", msg.get("info.missing." + key, "label", label));
        }
    }

    public static String travelText(Race race) {
        if (race.getVehicle() == null) {
            return Enums.display(race.getTravel());
        }
        String vehicle = race.getVehicle() == VehicleType.BOAT && race.getBoatWood() != BoatWood.OAK
                ? Enums.display(race.getBoatWood()) + " boat" : Enums.display(race.getVehicle());
        return Enums.display(race.getTravel()) + " (" + vehicle + ")";
    }

    private static void line(Messages msg, Audience to, String name, Location location) {
        line(msg, to, name, location == null ? null : Locations.describe(location));
    }

    private static void line(Messages msg, Audience to, String name, String value) {
        msg.sendRaw(to, "info.line", "name", name, "value", value == null ? msg.get("info.not-set") : value);
    }

    private static String describe(Region region) {
        return region == null ? null : region.describe();
    }
}
