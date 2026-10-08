package com.nltarzan.races.command;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.runtime.RaceListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * /nltraces (aliases /races, /nltr). Player subcommands (lobby) need nltraces.user;
 * everything else needs nltraces.admin, except that a race leader may use "cancel" for their own race.
 */
public final class NltRacesCommand implements TabExecutor {

    static final String ADMIN_PERMISSION = "nltraces.admin";
    private static final Set<String> PLAYER_SUBCOMMANDS = Set.of("lobby", "help");

    private final NltRacesPlugin plugin;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    public NltRacesCommand(NltRacesPlugin plugin) {
        this.plugin = plugin;
        register(new SubCommand("help", "help", "show this list", false,
                (sender, label, args) -> sendHelp(sender, label), SubCommand.NONE));
        register(new SubCommand("lobby", "lobby", "leave your race and go back to its lobby", true,
                this::lobby, SubCommand.NONE));
        new RaceCommands(plugin).subcommands().forEach(this::register);
        new SetupCommands(plugin).subcommands().forEach(this::register);
    }

    private void register(SubCommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }

    private static String permissionFor(String name) {
        return PLAYER_SUBCOMMANDS.contains(name) ? RaceListener.USER_PERMISSION : ADMIN_PERMISSION;
    }

    /** "cancel" is also open to a player while they're leading a race (to cancel that race). */
    private boolean mayUse(CommandSender sender, String name) {
        if (sender.hasPermission(permissionFor(name))) {
            return true;
        }
        return name.equals("cancel") && sender instanceof Player player
                && player.hasPermission(RaceListener.USER_PERMISSION)
                && plugin.getRaceService().leadingRace(player) != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        SubCommand subcommand = subcommands.get(args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            messages.send(sender, "general.unknown-command", "label", label);
            return true;
        }
        if (!mayUse(sender, subcommand.name())) {
            messages.send(sender, "general.no-permission");
            return true;
        }
        if (subcommand.playerOnly() && !(sender instanceof Player)) {
            messages.send(sender, "general.players-only");
            return true;
        }
        String[] rest = args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length);
        subcommand.handler().run(sender, label, rest);
        return true;
    }

    private void lobby(CommandSender sender, String label, String[] args) {
        if (!plugin.getRaceService().leave((Player) sender)) {
            plugin.getMessages().send(sender, "play.not-in-race");
        }
    }

    /** Lists only the subcommands the sender is allowed to use. */
    private void sendHelp(CommandSender sender, String label) {
        Messages messages = plugin.getMessages();
        messages.send(sender, "help.header", "version", plugin.getPluginMeta().getVersion());
        for (SubCommand subcommand : subcommands.values()) {
            if (mayUse(sender, subcommand.name())) {
                messages.sendRaw(sender, "help.line", "label", label, "usage", subcommand.usage(),
                        "description", subcommand.description());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> allowed = subcommands.keySet().stream().filter(name -> mayUse(sender, name)).toList();
            return CommandGroup.filter(allowed, args[0]);
        }
        SubCommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        return subcommand == null || !mayUse(sender, subcommand.name()) ? List.of()
                : subcommand.completer().complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
