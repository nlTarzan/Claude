package com.nltarzan.structures.command;

import com.nltarzan.structures.ResettingStructures;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /structure &lt;structure&gt; ...: the root command. Everything after the structure name goes to
 * that structure's own command, e.g. /structure monument status. Other structures (mansions, end
 * cities, ...) are added with {@link #register}.
 */
public final class StructureCommand implements BasicCommand {

    /** The commands of one structure. Gets the arguments after the structure name. */
    public interface Subcommand {
        void execute(CommandSourceStack source, String[] args);

        Collection<String> suggest(CommandSourceStack source, String[] args);
    }

    private final Map<String, Subcommand> structures = new LinkedHashMap<>();

    public StructureCommand register(String name, Subcommand command) {
        structures.put(name.toLowerCase(Locale.ROOT), command);
        return this;
    }

    @Override
    public String permission() {
        return ResettingStructures.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        Subcommand command = args.length == 0 ? null : structures.get(args[0].toLowerCase(Locale.ROOT));
        if (command == null) {
            source.getSender().sendMessage(Component.text("Usage: /structure <" + String.join("|", structures.keySet())
                    + "> ... (every command is explained in plugins/ResettingStructures/commands.txt)", NamedTextColor.RED));
            return;
        }
        command.execute(source, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return structures.keySet().stream().filter(name -> name.startsWith(prefix)).toList();
        }
        Subcommand command = structures.get(args[0].toLowerCase(Locale.ROOT));
        return command == null ? List.of() : command.suggest(source, Arrays.copyOfRange(args, 1, args.length));
    }
}
