package com.nltarzan.races.command;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.race.BlockPos;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceFormat;
import com.nltarzan.races.race.RaceRegistry.ButtonLink;
import com.nltarzan.races.setup.SetupService;
import com.nltarzan.races.setup.SetupSteps;
import com.nltarzan.races.util.Enums;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** setup (guided, right-click), set (one location or region, right-click), link and unlink. */
final class SetupCommands extends CommandGroup {

    private static final int LINK_REACH = 5;
    private static final double MIN_ZONE_RADIUS = 0.5;
    private static final double MAX_ZONE_RADIUS = 50.0;
    private static final int MAX_GRID_PLAYERS = 100;

    private static final List<String> SET_TARGETS = List.of("lobby", "start", "podium", "grid", "checkpoint",
            "pointzone");
    private static final String SET_USAGE = "set <race> <lobby|start|podium|grid|checkpoint|pointzone> ...";

    SetupCommands(NltRacesPlugin plugin) {
        super(plugin);
    }

    private SetupService setup() {
        return plugin.getSetupService();
    }

    @Override
    List<SubCommand> subcommands() {
        return List.of(
                new SubCommand("setup", "setup <race> [back|next|finish|start]",
                        "guided setup: right-click each location", true, this::setup,
                        (sender, args) -> args.length == 2
                                ? filter(List.of("back", "undo", "next", "finish", "start"), args[1])
                                : completeRace(args)),
                new SubCommand("set", SET_USAGE, "change one location or region by right-clicking",
                        true, this::set, this::completeSet),
                new SubCommand("link", "link <race> <prep|start|join|lobby|gate>",
                        "link the block you look at to a race action", true, this::link,
                        (sender, args) -> args.length == 2
                                ? filter(Enums.options(ButtonAction.class), args[1]) : completeRace(args)),
                new SubCommand("unlink", "unlink", "unlink the block you look at",
                        true, this::unlink, SubCommand.NONE));
    }

    // ---------------------------------------------------------------- setup

    private void setup(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "back", "undo", "next", "finish" -> {
                Race race = race(sender, label, args, "setup <race> " + action);
                if (race == null) {
                    return;
                }
                switch (action) {
                    case "back" -> setup().back(player, race, label);
                    case "undo" -> setup().undo(player, race, label);
                    case "next" -> setup().next(player, race, label);
                    default -> setup().finish(player, race, label);
                }
            }
            case "", "start" -> {
                Race race = editableRace(sender, label, args, "setup <race> [back|next|finish|start]");
                if (race != null) {
                    setup().startGuided(player, race, label, action.equals("start"));
                }
            }
            default -> msg().send(sender, "general.usage", "label", label,
                    "usage", "setup <race> [back|next|finish|start]");
        }
    }

    // ---------------------------------------------------------------- set

    private void set(CommandSender sender, String label, String[] args) {
        Race race = editableRace(sender, label, args, SET_USAGE);
        if (race == null || !hasArgs(sender, label, args, 2, SET_USAGE)) {
            return;
        }
        Player player = (Player) sender;
        String target = args[1].toLowerCase(Locale.ROOT);
        String option = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "";
        switch (target) {
            case "lobby" -> setup().startSingle(player, race, label, SetupSteps.lobby());
            case "start" -> {
                noteIfUnused(sender, race, race.getFormat() == RaceFormat.SOLO);
                setup().startSingle(player, race, label, SetupSteps.soloStart());
            }
            case "podium" -> {
                Integer place = args.length > 2 ? integer(sender, args[2]) : null;
                if (place == null || place < 1 || place > 3) {
                    msg().send(sender, "setup.invalid-podium");
                    return;
                }
                noteIfUnused(sender, race, race.getFormat().isMultiplayer());
                setup().startSingle(player, race, label, SetupSteps.podium(place));
            }
            case "grid" -> setGrid(player, race, label, option, args);
            case "checkpoint" -> setCheckpoint(player, race, label, option, args);
            case "pointzone" -> setPointZone(player, race, label, option, args);
            default -> msg().send(sender, "general.usage", "label", label, "usage", SET_USAGE);
        }
    }

    private void setGrid(Player player, Race race, String label, String option, String[] args) {
        noteIfUnused(player, race, race.getFormat().isMultiplayer());
        switch (option) {
            case "region" -> setup().startSingle(player, race, label, SetupSteps.gridRegion());
            case "addslot" -> setup().startSingle(player, race, label, SetupSteps.gridSlot(false));
            case "max" -> {
                Integer max = args.length > 3 ? integer(player, args[3]) : null;
                if (max == null) {
                    msg().send(player, "general.usage", "label", label, "usage", "set <race> grid max <players>");
                    return;
                }
                if (max < 1 || max > MAX_GRID_PLAYERS) {
                    msg().send(player, "set.out-of-range", "key", "max", "min", "1",
                            "max", String.valueOf(MAX_GRID_PLAYERS));
                    return;
                }
                race.setGridMax(max);
                races().save(race);
                msg().send(player, "grid.max-set", "race", race.getName(), "max", String.valueOf(max));
            }
            case "clear" -> {
                race.setGridRegion(null);
                race.getGridSlots().clear();
                races().save(race);
                msg().send(player, "grid.cleared", "race", race.getName());
            }
            default -> msg().send(player, "general.usage", "label", label,
                    "usage", "set <race> grid <region|addslot|max|clear>");
        }
    }

    private void setCheckpoint(Player player, Race race, String label, String option, String[] args) {
        noteIfUnused(player, race, race.getFormat() != RaceFormat.POINTS);
        String usage = "set <race> checkpoint <start|finish|add|remove|clear>";
        switch (option) {
            case "start" -> setup().startSingle(player, race, label, SetupSteps.startCheckpoint());
            case "finish" -> setup().startSingle(player, race, label, SetupSteps.finishCheckpoint());
            case "add" -> setup().startSingle(player, race, label, SetupSteps.middleCheckpoint(false));
            case "remove" -> {
                Integer index = listIndex(player, label, args, race.getCheckpoints().size(),
                        "set <race> checkpoint remove <number>", "checkpoint.invalid-number", race);
                if (index != null) {
                    race.getCheckpoints().remove((int) index);
                    races().save(race);
                    msg().send(player, "checkpoint.removed", "race", race.getName(),
                            "number", String.valueOf(index + 1));
                }
            }
            case "clear" -> {
                race.getCheckpoints().clear();
                races().save(race);
                msg().send(player, "checkpoint.cleared", "race", race.getName());
            }
            default -> msg().send(player, "general.usage", "label", label, "usage", usage);
        }
    }

    private void setPointZone(Player player, Race race, String label, String option, String[] args) {
        noteIfUnused(player, race, race.getFormat() == RaceFormat.POINTS);
        switch (option) {
            case "add" -> {
                double radius = SetupSteps.DEFAULT_ZONE_RADIUS;
                if (args.length > 3) {
                    try {
                        radius = Double.parseDouble(args[3]);
                    } catch (NumberFormatException e) {
                        msg().send(player, "general.invalid-number", "value", args[3]);
                        return;
                    }
                    if (radius < MIN_ZONE_RADIUS || radius > MAX_ZONE_RADIUS) {
                        msg().send(player, "set.out-of-range", "key", "radius",
                                "min", String.valueOf(MIN_ZONE_RADIUS), "max", String.valueOf(MAX_ZONE_RADIUS));
                        return;
                    }
                }
                setup().startSingle(player, race, label, SetupSteps.pointZone(false, radius));
            }
            case "remove" -> {
                Integer index = listIndex(player, label, args, race.getPointZones().size(),
                        "set <race> pointzone remove <number>", "pointzone.invalid-number", race);
                if (index != null) {
                    race.getPointZones().remove((int) index);
                    races().save(race);
                    msg().send(player, "pointzone.removed", "race", race.getName(),
                            "number", String.valueOf(index + 1));
                }
            }
            case "clear" -> {
                race.getPointZones().clear();
                races().save(race);
                msg().send(player, "pointzone.cleared", "race", race.getName());
            }
            default -> msg().send(player, "general.usage", "label", label,
                    "usage", "set <race> pointzone <add [radius]|remove|clear>");
        }
    }

    /** Reads a 1-based list number from args[3] and returns it 0-based, or null after telling the admin. */
    private Integer listIndex(CommandSender sender, String label, String[] args, int size, String usage,
                              String invalidKey, Race race) {
        if (!hasArgs(sender, label, args, 4, usage)) {
            return null;
        }
        Integer number = integer(sender, args[3]);
        if (number == null) {
            return null;
        }
        if (number < 1 || number > size) {
            msg().send(sender, invalidKey, "number", String.valueOf(number), "race", race.getName(),
                    "count", String.valueOf(size));
            return null;
        }
        return number - 1;
    }

    private List<String> completeSet(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> completeRace(args);
            case 2 -> filter(SET_TARGETS, args[1]);
            case 3 -> switch (args[1].toLowerCase(Locale.ROOT)) {
                case "podium" -> filter(List.of("1", "2", "3"), args[2]);
                case "grid" -> filter(List.of("region", "addslot", "max", "clear"), args[2]);
                case "checkpoint" -> filter(List.of("start", "finish", "add", "remove", "clear"), args[2]);
                case "pointzone" -> filter(List.of("add", "remove", "clear"), args[2]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    // ---------------------------------------------------------------- link / unlink

    private void link(CommandSender sender, String label, String[] args) {
        String usage = "link <race> <prep|start|join|lobby|gate>";
        Race race = editableRace(sender, label, args, usage);
        if (race == null || !hasArgs(sender, label, args, 2, usage)) {
            return;
        }
        ButtonAction action = option(sender, ButtonAction.class, "button action", args[1]);
        Block block = action == null ? null : targetBlock(sender);
        if (block == null) {
            return;
        }
        if (action == ButtonAction.GATE && block.getType() != Material.LEVER) {
            msg().send(sender, "link.gate-needs-lever");
            return;
        }
        BlockPos pos = BlockPos.of(block);
        ButtonLink existing = races().findButton(pos);
        if (existing != null) {
            msg().send(sender, "link.already-linked", "race", existing.race().getName(),
                    "action", Enums.display(existing.action()), "label", label);
            return;
        }
        race.getButtons().put(pos, action);
        races().save(race);
        msg().send(sender, "link.linked", "block", block.getType().name().toLowerCase(Locale.ROOT),
                "action", Enums.display(action), "race", race.getName());
        noteIfUnused(sender, race, action.appliesTo(race.getFormat()));
    }

    private void unlink(CommandSender sender, String label, String[] args) {
        Block block = targetBlock(sender);
        if (block == null) {
            return;
        }
        ButtonLink existing = races().findButton(BlockPos.of(block));
        if (existing == null) {
            msg().send(sender, "link.not-linked");
            return;
        }
        if (plugin.getRaceService().isActive(existing.race())) {
            msg().send(sender, "general.race-active", "race", existing.race().getName());
            return;
        }
        existing.race().getButtons().remove(BlockPos.of(block));
        races().save(existing.race());
        msg().send(sender, "link.unlinked", "action", Enums.display(existing.action()),
                "race", existing.race().getName());
    }

    private Block targetBlock(CommandSender sender) {
        Block block = ((Player) sender).getTargetBlockExact(LINK_REACH);
        if (block == null || block.getType().isAir()) {
            msg().send(sender, "link.no-block", "reach", String.valueOf(LINK_REACH));
            return null;
        }
        return block;
    }
}
