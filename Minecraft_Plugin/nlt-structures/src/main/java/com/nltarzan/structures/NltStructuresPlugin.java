package com.nltarzan.structures;

import com.nltarzan.structures.command.StructureCommand;
import com.nltarzan.structures.config.Messages;
import com.nltarzan.structures.info.InfoFiles;
import com.nltarzan.structures.monument.Monuments;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * nlT-Structures: custom-built versions of vanilla structures that reset with a new random
 * layout once a player has cleared them. Each structure is a {@link StructureModule} with its own
 * config file and /nltstructures subcommand; so far that's the Ocean Monument ({@link Monuments}).
 */
public final class NltStructuresPlugin extends JavaPlugin {

    /** Needed for /nltstructures and to receive the broadcast messages. Not given to anyone by default. */
    public static final String ADMIN_PERMISSION = "nltstructures.admin";
    /** The root command; every structure's commands start with it. */
    public static final String COMMAND = "/nltstructures";

    private List<StructureModule> modules = List.of();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        Messages messages = new Messages(this);
        InfoFiles infoFiles = new InfoFiles(this);

        // Every structure the plugin manages. A new structure is one more entry here.
        modules = List.of(new Monuments(this, messages, infoFiles::writeStatus));

        StructureCommand structureCommand = new StructureCommand(this, messages);
        for (StructureModule module : modules) {
            module.enable();
            infoFiles.add(module);
            structureCommand.register(module);
        }
        registerCommand(COMMAND.substring(1), "nlT-Structures commands, e.g. " + COMMAND + " monument status.",
                List.of("structures", "nlts"), structureCommand);

        infoFiles.writeCommands();
        infoFiles.writeStatus();
    }

    @Override
    public void onDisable() {
        for (StructureModule module : modules) {
            module.disable();
        }
        for (World world : getServer().getWorlds()) {
            world.removePluginChunkTickets(this);
        }
    }
}
