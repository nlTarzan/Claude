package com.nltarzan.structures;

import com.nltarzan.structures.command.StructureCommand;
import com.nltarzan.structures.info.InfoFiles;
import com.nltarzan.structures.monument.Monuments;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * ResettingStructures: custom-built versions of vanilla structures that reset with a new random
 * layout once a player has cleared them. Each structure is a {@link StructureModule} with its own
 * config file and /structure subcommand; so far that's the Ocean Monument ({@link Monuments}).
 */
public final class ResettingStructures extends JavaPlugin {

    /** Needed for /structure and to receive the broadcast messages. Not given to anyone by default. */
    public static final String ADMIN_PERMISSION = "structures_admin";

    private List<StructureModule> modules = List.of();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        InfoFiles infoFiles = new InfoFiles(this);

        // Every structure the plugin manages. A new structure is one more entry here.
        modules = List.of(new Monuments(this, infoFiles::writeStatus));

        StructureCommand structureCommand = new StructureCommand();
        for (StructureModule module : modules) {
            module.enable();
            infoFiles.add(module);
            structureCommand.register(module.commandName(), module.command());
        }
        registerCommand("structure", "Manage the resetting structures, e.g. /structure monument status.", List.of(),
                structureCommand);

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
