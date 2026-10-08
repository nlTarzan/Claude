package com.nltarzan.races.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * One /nltraces subcommand. Handlers and completers receive the arguments after the subcommand name.
 *
 * @param playerOnly true when the command uses the sender's position or view (sender is then a Player)
 */
public record SubCommand(String name, String usage, String description, boolean playerOnly,
                         Handler handler, Completer completer) {

    @FunctionalInterface
    public interface Handler {
        void run(CommandSender sender, String label, String[] args);
    }

    @FunctionalInterface
    public interface Completer {
        List<String> complete(CommandSender sender, String[] args);
    }

    static final Completer NONE = (sender, args) -> List.of();
}
