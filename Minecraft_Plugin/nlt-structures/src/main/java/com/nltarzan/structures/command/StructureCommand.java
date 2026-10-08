package com.nltarzan.structures.command;

import com.nltarzan.structures.NltStructuresPlugin;
import com.nltarzan.structures.StructureModule;
import com.nltarzan.structures.config.Messages;
import com.nltarzan.structures.info.CommandHelp;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * /nltstructures (aliases /structures, /nlts): the root command. "help" and "reload" are handled
 * here; everything after a structure name goes to that structure's own command, e.g.
 * /nltstructures monument status. Other structures (mansions, end cities, ...) are added with
 * {@link #register}.
 */
public final class StructureCommand implements BasicCommand {

    /** The commands of one structure. Gets the arguments after the structure name. */
    public interface Subcommand {
        void execute(CommandSourceStack source, String[] args);

        Collection<String> suggest(CommandSourceStack source, String[] args);
    }

    private static final List<String> OWN_SUBCOMMANDS = List.of("help", "reload");

    private final JavaPlugin plugin;
    private final Messages messages;
    private final Map<String, StructureModule> structures = new LinkedHashMap<>();

    public StructureCommand(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public StructureCommand register(StructureModule module) {
        structures.put(module.commandName().toLowerCase(Locale.ROOT), module);
        return this;
    }

    @Override
    public String permission() {
        return NltStructuresPlugin.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String name = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (name) {
            case "help" -> sendHelp(sender);
            case "reload" -> {
                messages.reload();
                messages.send(sender, "general.reloaded", "command", NltStructuresPlugin.COMMAND);
            }
            default -> {
                StructureModule module = structures.get(name);
                if (module == null) {
                    messages.send(sender, "general.unknown-command", "command", NltStructuresPlugin.COMMAND);
                    return;
                }
                module.command().execute(source, Arrays.copyOfRange(args, 1, args.length));
            }
        }
    }

    /** One line per command: the usage and the first sentence of its explanation (the rest is in commands.txt). */
    private void sendHelp(CommandSender sender) {
        messages.send(sender, "help.header", "version", plugin.getPluginMeta().getVersion());
        messages.sendRaw(sender, "help.line", "usage", NltStructuresPlugin.COMMAND + " help",
                "description", "show this list");
        messages.sendRaw(sender, "help.line", "usage", NltStructuresPlugin.COMMAND + " reload",
                "description", "re-read messages.yml");
        for (StructureModule module : structures.values()) {
            for (CommandHelp help : module.commandHelp()) {
                messages.sendRaw(sender, "help.line", "usage", help.usage(),
                        "description", firstSentence(help.description()));
            }
        }
        messages.sendRaw(sender, "help.footer");
    }

    private static String firstSentence(String text) {
        int end = text.indexOf(". ");
        return end < 0 ? text : text.substring(0, end + 1);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return Stream.concat(OWN_SUBCOMMANDS.stream(), structures.keySet().stream())
                    .filter(name -> name.startsWith(prefix)).toList();
        }
        StructureModule module = structures.get(args[0].toLowerCase(Locale.ROOT));
        return module == null ? List.of()
                : module.command().suggest(source, Arrays.copyOfRange(args, 1, args.length));
    }
}
