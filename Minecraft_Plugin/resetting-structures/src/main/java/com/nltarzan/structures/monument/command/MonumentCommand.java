package com.nltarzan.structures.monument.command;

import com.nltarzan.structures.command.StructureCommand;
import com.nltarzan.structures.info.CommandHelp;
import com.nltarzan.structures.info.StatusLine;
import com.nltarzan.structures.monument.Monuments;
import com.nltarzan.structures.monument.layout.LayoutPrinter;
import com.nltarzan.structures.monument.layout.MazeSettings;
import com.nltarzan.structures.monument.layout.MonumentLayout;
import com.nltarzan.structures.monument.CurrentMonument.Built;
import com.nltarzan.structures.monument.MonumentResetter;
import com.nltarzan.structures.monument.MonumentResetter.Result;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * /structure monument status | active [on|off] | check | reset [variant|generated] [seed] | setcenter [x y z]
 * | layout [current|seed|random] [newest-chance] | broadcast [on|off] | reload
 */
public final class MonumentCommand implements StructureCommand.Subcommand {

    static final String COMMAND = "/structure monument";
    /** The y-level an Ocean Monument is normally built at; setcenter reminds you when it differs. */
    private static final int USUAL_Y = 40;
    /** Every subcommand. MonumentCommandTest checks that each one is explained in HELP. */
    static final List<String> SUBCOMMANDS =
            List.of("status", "active", "check", "reset", "setcenter", "layout", "broadcast", "reload");

    /**
     * Every command with what it does, for commands.txt. Keep this in step with execute() below;
     * MonumentCommandTest fails when a subcommand is missing here.
     */
    public static final List<CommandHelp> HELP = List.of(
            new CommandHelp(COMMAND + " [status]",
                    "Shows the monument's location, what's built right now, its sponges, the Elder Guardians"
                            + " (when the area is loaded), the last check, the reset mode, when the next check"
                            + " is and whether broadcasts are on. The same information is in status.txt."),
            new CommandHelp(COMMAND + " active [on|off]",
                    "Turns the checks and resets on or off; without on or off it switches. It's off on a new"
                            + " server, so nothing is built until you've checked the location in monuments.yml"
                            + " (or used setcenter) and turned it on. Saved, so it stays on or off after restarts."
                            + " While off, the Monument Hoe rules are off too.",
                    List.of(COMMAND + " active on", COMMAND + " active off")),
            new CommandHelp(COMMAND + " check",
                    "Checks the monument right away, just like the timer does: it only resets when every Elder"
                            + " Guardian is dead and no player is inside. Afterwards a new random wait starts."),
            new CommandHelp(COMMAND + " reset",
                    "Places a new monument right away, even when Elder Guardians are alive or players are"
                            + " inside. What it places follows reset.mode in monuments.yml."),
            new CommandHelp(COMMAND + " reset <variant>",
                    "Places one specific pre-built monument right away. The variants are listed in"
                            + " monuments.yml.",
                    List.of(COMMAND + " reset monument_03")),
            new CommandHelp(COMMAND + " reset generated [seed]",
                    "Builds a generated monument right away, whatever reset.mode says. With a seed you get"
                            + " exactly that layout again (seeds are shown by status and layout).",
                    List.of(COMMAND + " reset generated", COMMAND + " reset generated 12345")),
            new CommandHelp(COMMAND + " layout [seed|random] [newest-chance]",
                    "Prints a generated layout as a map in the server console, without building anything."
                            + " The newest chance (0 to 1) overrides the one drawn from monuments.yml: 1 gives"
                            + " long corridors, 0 many short branches.",
                    List.of(COMMAND + " layout", COMMAND + " layout 12345", COMMAND + " layout random 0.9")),
            new CommandHelp(COMMAND + " layout current",
                    "Prints the layout of the generated monument that's built right now in the server console."),
            new CommandHelp(COMMAND + " setcenter",
                    "In-game only: moves the monument's center to the block corner you're standing on and"
                            + " saves it in monuments.yml. Stand where the 4 center blocks meet, on the level"
                            + " the rooms start (usually y=40)."),
            new CommandHelp(COMMAND + " setcenter <x> <y> <z>",
                    "Moves the monument's center to exactly these coordinates and saves it in monuments.yml."
                            + " In-game it uses the world you're in, from the console the world in monuments.yml."
                            + " A y-level other than 40 gets a reminder to double check it.",
                    List.of(COMMAND + " setcenter 224 40 -320")),
            new CommandHelp(COMMAND + " broadcast [on|off]",
                    "Turns the reset messages for players with the admin permission on or off. Without on or"
                            + " off it switches. Saved in monuments.yml."),
            new CommandHelp(COMMAND + " reload",
                    "Re-reads monuments.yml and the structure files. If monuments.yml has a mistake, the"
                            + " previous settings stay in use and the mistake is shown in the console and"
                            + " status.txt."));

    private final Monuments monuments;

    public MonumentCommand(Monuments monuments) {
        this.monuments = monuments;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> status(sender);
            case "active" -> active(sender, args);
            case "check" -> run(sender, false, null, null);
            case "reset" -> reset(sender, args);
            case "setcenter" -> setCenter(sender, source.getLocation(), args);
            case "layout" -> layout(sender, args);
            case "broadcast" -> broadcast(sender, args);
            case "reload" -> {
                if (monuments.reloadSettings()) {
                    sender.sendMessage(Component.text("Reloaded monuments.yml and the structures.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("monuments.yml " + monuments.configProblem() + ". "
                            + (monuments.isPaused() ? "The monument stays paused." : "Still using the previous settings.")
                            + " Details are in the console.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: " + COMMAND + " <" + String.join("|", SUBCOMMANDS)
                    + ">. Every command is explained in plugins/ResettingStructures/commands.txt.", NamedTextColor.RED));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Stream.concat(Stream.of(MonumentResetter.GENERATED), monuments.structures().names().stream())
                    .filter(s -> s.startsWith(args[1])).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("active"))) {
            return Stream.of("on", "off").filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("layout")) {
            return Stream.of("current", "random").filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length >= 2 && args.length <= 4 && args[0].equalsIgnoreCase("setcenter")
                && source.getSender() instanceof Player) {
            // Your own position (the nearest block corner), like vanilla commands suggest.
            Location here = source.getLocation();
            long[] xyz = {Math.round(here.getX()), here.getBlockY(), Math.round(here.getZ())};
            return List.of(String.valueOf(xyz[args.length - 2]));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("layout")) {
            return Stream.of("0", "0.5", "1").filter(s -> s.startsWith(args[2])).toList();
        }
        return List.of();
    }

    /** /structure monument broadcast [on|off]: without an argument it toggles. Saved in monuments.yml. */
    private void broadcast(CommandSender sender, String[] args) {
        boolean on;
        if (args.length < 2) {
            on = !monuments.settings().announce();
        } else if (args[1].equalsIgnoreCase("on")) {
            on = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            on = false;
        } else {
            sender.sendMessage(Component.text("Usage: " + COMMAND + " broadcast [on|off]", NamedTextColor.RED));
            return;
        }
        if (!monuments.setAnnounce(on)) {
            configProblem(sender);
            return;
        }
        sender.sendMessage(Component.text("Monument broadcasts are now " + (on ? "on" : "off") + ".",
                on ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

    /**
     * /structure monument layout [current|seed|random] [newest-chance]: prints a layout in the server console.
     * "current" shows the generated monument that's built right now. Otherwise a new layout is
     * generated; without a newest chance it's drawn from the configured curve like a real reset.
     */
    private void layout(CommandSender sender, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("current")) {
            Optional<Built> built = monuments.currentMonument().get();
            if (built.isEmpty()) {
                sender.sendMessage(Component.text("The plugin hasn't placed a monument yet.", NamedTextColor.YELLOW));
            } else if (built.get() instanceof Built.PreBuilt(String variant)) {
                sender.sendMessage(Component.text("The current monument is pre-built variant " + variant
                        + ", it has no generated layout.", NamedTextColor.YELLOW));
            } else if (built.get() instanceof Built.Generated generated) {
                printLayout(sender, generated.layout(), "Current layout");
            }
            return;
        }

        long seed = ThreadLocalRandom.current().nextLong();
        MazeSettings settings = monuments.settings().maze();
        try {
            if (args.length > 1 && !args[1].equalsIgnoreCase("random")) {
                seed = Long.parseLong(args[1]);
            }
            if (args.length > 2) {
                double chance = Double.parseDouble(args[2]);
                if (chance < 0 || chance > 1) {
                    throw new NumberFormatException();
                }
                settings = settings.withNewestChance(chance);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Usage: " + COMMAND + " layout [current|seed|random] [newest-chance 0-1]",
                    NamedTextColor.RED));
            return;
        }
        printLayout(sender, MonumentLayout.generate(seed, settings), "Layout");
    }

    private void printLayout(CommandSender sender, MonumentLayout layout, String what) {
        LayoutPrinter.print(layout).forEach(monuments.getLogger()::info);
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text(what + " " + layout.seed() + " (" + Math.round(layout.newestChance() * 100)
                    + "% newest) printed in the server console.", NamedTextColor.GREEN));
        }
    }

    /** /structure monument reset [variant|generated] [seed]: places a new monument right away. */
    private void reset(CommandSender sender, String[] args) {
        String variant = args.length > 1 ? args[1] : null;
        if (MonumentResetter.GENERATED.equalsIgnoreCase(variant)) {
            variant = MonumentResetter.GENERATED; // so "Generated" works too
        }
        Long seed = null;
        if (args.length > 2) {
            if (!MonumentResetter.GENERATED.equalsIgnoreCase(variant)) {
                // A seed only picks a generated layout; a pre-built variant has nothing to seed.
                sender.sendMessage(Component.text("A seed only works with " + COMMAND + " reset generated <seed>."
                        + " Nothing was reset.", NamedTextColor.RED));
                return;
            }
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Usage: " + COMMAND + " reset generated [seed]", NamedTextColor.RED));
                return;
            }
        }
        run(sender, true, variant, seed);
    }

    /** The status in color: a gray label and a colored value per line (the same lines as status.txt). */
    private void status(CommandSender sender) {
        sender.sendMessage(Component.text(monuments.name(), NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        for (StatusLine line : monuments.status()) {
            sender.sendMessage(Component.text(" " + line.label() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(line.value(), line.color())));
        }
    }

    /** /structure monument active [on|off]: without an argument it toggles. Saved in monuments/state.yml. */
    private void active(CommandSender sender, String[] args) {
        boolean wasOn = monuments.scheduler().isActive();
        boolean on;
        if (args.length < 2) {
            on = !wasOn;
        } else if (args[1].equalsIgnoreCase("on")) {
            on = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            on = false;
        } else {
            sender.sendMessage(Component.text("Usage: " + COMMAND + " active [on|off]", NamedTextColor.RED));
            return;
        }
        if (on == wasOn) {
            sender.sendMessage(Component.text("The monument is already " + (on ? "active" : "inactive") + ".",
                    NamedTextColor.YELLOW));
            return;
        }
        if (!monuments.setActive(on)) {
            sender.sendMessage(Component.text("Not turned on: monuments.yml " + monuments.configProblem()
                    + ". Fix it and run " + COMMAND + " reload first.", NamedTextColor.RED));
            return;
        }
        if (on) {
            var settings = monuments.settings();
            sender.sendMessage(Component.text("The monument is now active at " + settings.worldName() + " "
                    + settings.centerX() + " " + settings.centerY() + " " + settings.centerZ() + ". Use " + COMMAND
                    + " status to see when the first check is.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("The monument is now inactive: nothing is checked or built until "
                    + COMMAND + " active on.", NamedTextColor.YELLOW));
        }
    }

    private void run(CommandSender sender, boolean force, String variant, Long seed) {
        info(sender, force ? "Resetting the monument..." : "Checking the monument...");
        monuments.scheduler().runNow(force, variant, seed).thenAccept(result -> sender.sendMessage(switch (result) {
            case Result.NotCleared(int alive) -> Component.text(
                    "Not cleared: " + alive + " Elder Guardian(s) alive. Use " + COMMAND + " reset to force it.",
                    NamedTextColor.YELLOW);
            case Result.PlayersInside(int players) -> Component.text(
                    "Cleared, but " + players + " player(s) are inside. Postponed; use " + COMMAND + " reset to force it.",
                    NamedTextColor.YELLOW);
            case Result.Placed(String placed) -> Component.text("Placed " + placed + ".", NamedTextColor.GREEN);
            case Result.Failed(String reason) -> Component.text("Failed: " + reason, NamedTextColor.RED);
        }));
    }

    /**
     * /structure monument setcenter [x y z]: without coordinates the player's own position, with
     * coordinates exactly those (in the player's world, or from the console in the configured world).
     */
    private void setCenter(CommandSender sender, Location location, String[] args) {
        World world;
        int x;
        int y;
        int z;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("From the console, give coordinates: " + COMMAND
                        + " setcenter <x> <y> <z>", NamedTextColor.RED));
                return;
            }
            // The datapack's marker stood on the corner where the 4 center blocks meet, so round to the nearest corner.
            world = location.getWorld();
            x = (int) Math.round(location.getX());
            y = location.getBlockY();
            z = (int) Math.round(location.getZ());
        } else if (args.length == 4) {
            try {
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Coordinates must be whole numbers: " + COMMAND
                        + " setcenter <x> <y> <z>", NamedTextColor.RED));
                return;
            }
            world = sender instanceof Player
                    ? location.getWorld()
                    : monuments.getServer().getWorld(monuments.settings().worldName());
            if (world == null) {
                sender.sendMessage(Component.text("World '" + monuments.settings().worldName() + "' is not loaded.",
                        NamedTextColor.RED));
                return;
            }
        } else {
            sender.sendMessage(Component.text("Usage: " + COMMAND + " setcenter [<x> <y> <z>]", NamedTextColor.RED));
            return;
        }

        if (!monuments.setCenter(world, x, y, z)) {
            configProblem(sender);
            return;
        }
        sender.sendMessage(Component.text("Monument center set to " + x + " " + y + " " + z + " in "
                + world.getName() + ".", NamedTextColor.GREEN));
        if (y != USUAL_Y) {
            sender.sendMessage(Component.text("Double check your y-level. An Ocean Monument is usually set at y="
                    + USUAL_Y + ".", NamedTextColor.GOLD));
        }
    }

    /** monuments.yml has a mistake, so nothing is saved: saving would overwrite the file being fixed. */
    private void configProblem(CommandSender sender) {
        sender.sendMessage(Component.text("Not saved: monuments.yml " + monuments.configProblem()
                + ". Fix it and run " + COMMAND + " reload first.", NamedTextColor.RED));
    }

    private static void info(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GRAY));
    }
}
