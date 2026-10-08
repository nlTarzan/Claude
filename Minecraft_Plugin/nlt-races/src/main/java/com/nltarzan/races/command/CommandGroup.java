package com.nltarzan.races.command;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceRegistry;
import com.nltarzan.races.util.Enums;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Shared helpers for a set of related subcommands. */
abstract class CommandGroup {

    protected final NltRacesPlugin plugin;

    CommandGroup(NltRacesPlugin plugin) {
        this.plugin = plugin;
    }

    abstract List<SubCommand> subcommands();

    protected Messages msg() {
        return plugin.getMessages();
    }

    protected RaceRegistry races() {
        return plugin.getRaces();
    }

    /** Checks that at least {@code count} arguments were given, otherwise shows the usage. */
    protected boolean hasArgs(CommandSender sender, String label, String[] args, int count, String usage) {
        if (args.length >= count) {
            return true;
        }
        msg().send(sender, "general.usage", "label", label, "usage", usage);
        return false;
    }

    /** Looks up the race named in args[0], telling the sender if it's missing. */
    protected Race race(CommandSender sender, String label, String[] args, String usage) {
        if (!hasArgs(sender, label, args, 1, usage)) {
            return null;
        }
        Race race = races().get(args[0]);
        if (race == null) {
            msg().send(sender, "general.unknown-race", "race", args[0], "label", label);
        }
        return race;
    }

    /** Like {@link #race}, but refuses while the race is being run so it isn't changed under a racer. */
    protected Race editableRace(CommandSender sender, String label, String[] args, String usage) {
        Race race = race(sender, label, args, usage);
        if (race != null && plugin.getRaceService().isActive(race)) {
            msg().send(sender, "general.race-active", "race", race.getName());
            return null;
        }
        return race;
    }

    protected <E extends Enum<E>> E option(CommandSender sender, Class<E> type, String what, String input) {
        E value = Enums.parse(type, input);
        if (value == null) {
            msg().send(sender, "general.invalid-option", "what", what, "value", input,
                    "options", String.join(", ", Enums.options(type)));
        }
        return value;
    }

    protected Integer integer(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            msg().send(sender, "general.invalid-number", "value", input);
            return null;
        }
    }

    /** Reminds the admin when they set something this race's format doesn't use. */
    protected void noteIfUnused(CommandSender sender, Race race, boolean used) {
        if (!used) {
            msg().send(sender, "general.not-used", "race", race.getName(),
                    "format", Enums.display(race.getFormat()));
        }
    }

    protected List<String> completeRace(String[] args) {
        return args.length == 1 ? filter(races().names(), args[0]) : List.of();
    }

    protected static List<String> filter(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
