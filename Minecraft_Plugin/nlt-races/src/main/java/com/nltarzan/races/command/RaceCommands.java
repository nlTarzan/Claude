package com.nltarzan.races.command;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.race.BoatWood;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.RaceRegistry;
import com.nltarzan.races.race.RaceSetting;
import com.nltarzan.races.race.Rules;
import com.nltarzan.races.race.TravelMode;
import com.nltarzan.races.race.VehicleType;
import com.nltarzan.races.setup.RaceInfo;
import com.nltarzan.races.util.Enums;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** create, delete, list, info, edit, reload. */
final class RaceCommands extends CommandGroup {

    private static final List<String> ENUM_KEYS =
            List.of("name", "format", "travel", "vehicle", "boat", "rules", "checkpoint-particles");
    private static final int MAX_DISPLAY_NAME = 48;

    RaceCommands(NltRacesPlugin plugin) {
        super(plugin);
    }

    @Override
    List<SubCommand> subcommands() {
        return List.of(
                new SubCommand("create", "create <race> <format> <travel> [vehicle]",
                        "create a race and start its setup", false, this::create, this::completeCreate),
                new SubCommand("delete", "delete <race> confirm", "delete a race and all its times",
                        false, this::delete, (sender, args) -> args.length == 2
                        ? filter(List.of("confirm"), args[1]) : completeRace(args)),
                new SubCommand("list", "list", "list all races", false, this::list, SubCommand.NONE),
                new SubCommand("info", "info <race>", "show a race's setup and what's missing",
                        false, this::info, (sender, args) -> completeRace(args)),
                new SubCommand("edit", "edit <race> <setting> <value>", "change format, travel, vehicle, "
                        + "boat type, rules or a number setting", false, this::edit, this::completeEdit),
                new SubCommand("cancel", "cancel [race]", "stop selecting blocks, or cancel a race "
                        + "(leaders: the race you lead)", false, this::cancel, this::completeCancel),
                new SubCommand("cleartimes", "cleartimes <race> confirm", "delete all recorded times of a race "
                        + "(the race itself is kept)", false, this::clearTimes,
                        (sender, args) -> args.length == 2 ? filter(List.of("confirm"), args[1]) : completeRace(args)),
                new SubCommand("reload", "reload", "reload config, messages and race files",
                        false, this::reload, SubCommand.NONE));
    }

    // ---------------------------------------------------------------- cleartimes

    /** Deletes every recorded race and run of a race, after a (clickable) confirmation. The race is kept. */
    private void clearTimes(CommandSender sender, String label, String[] args) {
        Race race = editableRace(sender, label, args, "cleartimes <race> confirm");
        if (race == null) {
            return;
        }
        String name = race.getName();
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            String command = "/" + label + " cleartimes " + name + " confirm";
            msg().send(sender, "cleartimes.confirm", "race", race, "label", label,
                    "confirm", msg().button("cleartimes.button", command, "race", race));
            return;
        }
        plugin.getRunStore().deleteCourse(name).whenComplete((runs, error) -> plugin.sync(() -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not clear the times of race " + name, error);
                msg().send(sender, "cleartimes.failed", "race", race);
                return;
            }
            Race current = races().get(name);
            if (current != null) {
                plugin.getLeaderboards().refresh(current);
            }
            msg().send(sender, "cleartimes.done", "race", race, "runs", String.valueOf(runs));
        }));
    }

    // ---------------------------------------------------------------- cancel

    /**
     * Without a race: stops the block selection (set or setup) the admin is in; otherwise a race leader
     * cancels the race they lead. With a race: admins cancel any active race, leaders only their own.
     * Cancelling sends everyone to the lobby and records nothing.
     */
    private void cancel(CommandSender sender, String label, String[] args) {
        boolean admin = sender.hasPermission(NltRacesCommand.ADMIN_PERMISSION);
        Player player = sender instanceof Player p ? p : null;
        Race leading = player == null ? null : plugin.getRaceService().leadingRace(player);
        if (args.length == 0) {
            if (player == null) {
                msg().send(sender, "general.usage", "label", label, "usage", "cancel <race>");
            } else if (admin && plugin.getSetupService().cancelSelection(player)) {
                return; // stopped the block selection
            } else if (leading != null) {
                plugin.getRaceService().cancelByLeader(player);
            } else {
                msg().send(sender, "cancel.nothing", "label", label);
            }
            return;
        }
        Race race = race(sender, label, args, "cancel [race]");
        if (race == null) {
            return;
        }
        if (!admin) {
            if (race == leading) {
                plugin.getRaceService().cancelByLeader(player);
            } else {
                msg().send(sender, "general.no-permission");
            }
            return;
        }
        int sent = plugin.getRaceService().cancel(race);
        if (sent < 0) {
            msg().send(sender, "cancel.not-running", "race", race);
        } else {
            msg().send(sender, "cancel.race", "race", race, "count", String.valueOf(sent));
        }
    }

    /** Admins: every race. Leaders: only the race they lead. */
    private List<String> completeCancel(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        if (sender.hasPermission(NltRacesCommand.ADMIN_PERMISSION)) {
            return completeRace(args);
        }
        Race leading = sender instanceof Player player ? plugin.getRaceService().leadingRace(player) : null;
        return leading == null ? List.of() : filter(List.of(leading.getName()), args[0]);
    }

    // ---------------------------------------------------------------- create / delete / list / info

    private void create(CommandSender sender, String label, String[] args) {
        String usage = "create <race> <format> <travel> [vehicle]";
        if (!hasArgs(sender, label, args, 3, usage)) {
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!RaceRegistry.isValidName(name)) {
            msg().send(sender, "race.invalid-name");
            return;
        }
        if (races().get(name) != null) {
            msg().send(sender, "race.exists", "race", name);
            return;
        }
        RaceFormat format = option(sender, RaceFormat.class, "format", args[1]);
        TravelMode travel = format == null ? null : option(sender, TravelMode.class, "travel mode", args[2]);
        if (travel == null) {
            return;
        }
        VehicleType vehicle = null;
        if (travel == TravelMode.VEHICLE) {
            if (args.length < 4) {
                msg().send(sender, "race.vehicle-required",
                        "options", String.join(", ", Enums.options(VehicleType.class)));
                return;
            }
            vehicle = option(sender, VehicleType.class, "vehicle", args[3]);
            if (vehicle == null) {
                return;
            }
        }
        Race race = races().create(name, format, travel, vehicle);
        msg().send(sender, "race.created", "race", race, "format", Enums.display(format),
                "travel", RaceInfo.travelText(race), "label", label);
        if (sender instanceof Player player) {
            // A new race goes through every step, including optional ones like middle checkpoints.
            plugin.getSetupService().startGuided(player, race, label, true);
        }
    }

    private List<String> completeCreate(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> filter(Enums.options(RaceFormat.class), args[1]);
            case 3 -> filter(Enums.options(TravelMode.class), args[2]);
            case 4 -> args[2].equalsIgnoreCase("vehicle")
                    ? filter(Enums.options(VehicleType.class), args[3]) : List.of();
            default -> List.of();
        };
    }

    private void delete(CommandSender sender, String label, String[] args) {
        Race race = editableRace(sender, label, args, "delete <race> confirm");
        if (race == null) {
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            String command = "/" + label + " delete " + race.getName() + " confirm";
            msg().send(sender, "race.delete-confirm", "race", race, "label", label,
                    "confirm", msg().button("race.delete-button", command, "race", race));
            return;
        }
        String name = race.getName();
        plugin.getSetupService().cancelFor(race);
        races().delete(name);
        plugin.getLeaderboards().remove(name);
        plugin.getRunStore().deleteCourse(name).whenComplete((runs, error) -> plugin.sync(() -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete the times of race " + name, error);
                msg().send(sender, "race.deleted-times-failed", "race", name);
            } else {
                msg().send(sender, "race.deleted", "race", name, "runs", String.valueOf(runs));
            }
        }));
    }

    private void list(CommandSender sender, String label, String[] args) {
        if (races().all().isEmpty()) {
            msg().send(sender, "list.empty", "label", label);
            return;
        }
        msg().send(sender, "list.header", "count", String.valueOf(races().all().size()));
        for (Race race : races().all()) {
            msg().sendRaw(sender, "list.line", "race", race,
                    "format", Enums.display(race.getFormat()), "travel", RaceInfo.travelText(race),
                    "status", msg().get(race.isReady() ? "list.ready" : "list.not-ready"));
        }
    }

    private void info(CommandSender sender, String label, String[] args) {
        Race race = race(sender, label, args, "info <race>");
        if (race != null) {
            RaceInfo.show(plugin, sender, race, label);
        }
    }

    // ---------------------------------------------------------------- edit

    private void edit(CommandSender sender, String label, String[] args) {
        String usage = "edit <race> <setting> <value>";
        Race race = editableRace(sender, label, args, usage);
        if (race == null || !hasArgs(sender, label, args, 3, usage)) {
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        String value = args[2];
        switch (key) {
            case "format" -> {
                RaceFormat format = option(sender, RaceFormat.class, "format", value);
                if (format == null) {
                    return;
                }
                race.setFormat(format);
            }
            case "travel" -> {
                TravelMode travel = option(sender, TravelMode.class, "travel mode", value);
                if (travel == null) {
                    return;
                }
                race.setTravel(travel);
            }
            case "vehicle" -> {
                if (race.getTravel() != TravelMode.VEHICLE) {
                    msg().send(sender, "set.vehicle-needs-travel", "race", race, "label", label);
                    return;
                }
                VehicleType vehicle = option(sender, VehicleType.class, "vehicle", value);
                if (vehicle == null) {
                    return;
                }
                race.setVehicle(vehicle);
            }
            case "name" -> {
                // Everything after "name" is the display name, so it can contain spaces.
                String customName = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
                if (customName.length() > MAX_DISPLAY_NAME) {
                    msg().send(sender, "set.name-too-long", "max", String.valueOf(MAX_DISPLAY_NAME));
                    return;
                }
                race.setCustomName(customName.equalsIgnoreCase("reset") ? null : customName);
                races().save(race);
                msg().send(sender, "set.name-updated", "race", race);
                return;
            }
            case "checkpoint-particles" -> {
                switch (value.toLowerCase(Locale.ROOT)) {
                    case "on", "true" -> race.setCheckpointParticles(true);
                    case "off", "false" -> race.setCheckpointParticles(false);
                    case "auto" -> race.setCheckpointParticles(null);
                    default -> {
                        msg().send(sender, "general.invalid-option", "what", "value", "value", value,
                                "options", "on, off, auto");
                        return;
                    }
                }
                noteIfUnused(sender, race, race.getFormat() != RaceFormat.POINTS);
            }
            case "boat" -> {
                BoatWood wood = option(sender, BoatWood.class, "boat type", value);
                if (wood == null) {
                    return;
                }
                race.setBoatWood(wood);
                noteIfUnused(sender, race, race.getVehicle() == VehicleType.BOAT);
            }
            case "rules" -> {
                Rules rules = option(sender, Rules.class, "rules", value);
                if (rules == null) {
                    return;
                }
                race.setRules(rules);
                noteIfUnused(sender, race, race.getTravel().hasRules());
            }
            default -> {
                RaceSetting setting = RaceSetting.byKey(key);
                if (setting == null) {
                    msg().send(sender, "set.unknown-key", "key", key, "options", String.join(", ", editKeys()));
                    return;
                }
                Integer number = integer(sender, value);
                if (number == null) {
                    return;
                }
                if (number < setting.min() || number > setting.max()) {
                    msg().send(sender, "set.out-of-range", "key", setting.key(),
                            "min", String.valueOf(setting.min()), "max", String.valueOf(setting.max()));
                    return;
                }
                race.setSetting(setting, number);
                noteIfUnused(sender, race, setting.appliesTo(race));
            }
        }
        races().save(race);
        msg().send(sender, "set.updated", "race", race, "key", key, "value", value.toLowerCase(Locale.ROOT));
        if (race.getTravel() == TravelMode.VEHICLE && race.getVehicle() == null) {
            msg().send(sender, "race.vehicle-required",
                    "options", String.join(", ", Enums.options(VehicleType.class)));
        }
    }

    private static List<String> editKeys() {
        List<String> keys = new ArrayList<>(ENUM_KEYS);
        Arrays.stream(RaceSetting.values()).map(RaceSetting::key).forEach(keys::add);
        return keys;
    }

    private List<String> completeEdit(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> completeRace(args);
            case 2 -> filter(editKeys(), args[1]);
            case 3 -> switch (args[1].toLowerCase(Locale.ROOT)) {
                case "format" -> filter(Enums.options(RaceFormat.class), args[2]);
                case "travel" -> filter(Enums.options(TravelMode.class), args[2]);
                case "vehicle" -> filter(Enums.options(VehicleType.class), args[2]);
                case "boat" -> filter(Enums.options(BoatWood.class), args[2]);
                case "rules" -> filter(Enums.options(Rules.class), args[2]);
                case "checkpoint-particles" -> filter(List.of("on", "off", "auto"), args[2]);
                case "name" -> filter(List.of("reset"), args[2]);
                default -> currentValue(args[0], args[1]); // number settings: suggest the current value
            };
            default -> List.of();
        };
    }

    private List<String> currentValue(String raceName, String key) {
        Race race = races().get(raceName);
        RaceSetting setting = RaceSetting.byKey(key);
        return race == null || setting == null ? List.of() : List.of(String.valueOf(race.getSetting(setting)));
    }

    // ---------------------------------------------------------------- reload

    private void reload(CommandSender sender, String label, String[] args) {
        if (plugin.getRaceService().hasActiveRaces()) {
            msg().send(sender, "general.reload-while-racing");
            return;
        }
        plugin.getSetupService().cancelAll(); // the race objects are about to be replaced
        String configProblem = plugin.reload();
        String count = String.valueOf(races().all().size());
        if (configProblem == null) {
            msg().send(sender, "general.reloaded", "count", count);
        } else {
            msg().send(sender, "general.reload-failed", "problem", configProblem, "count", count);
        }
    }
}
