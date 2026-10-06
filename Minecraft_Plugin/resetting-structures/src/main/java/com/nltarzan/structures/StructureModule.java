package com.nltarzan.structures;

import com.nltarzan.structures.command.StructureCommand;
import com.nltarzan.structures.info.CommandHelp;
import com.nltarzan.structures.info.StatusLine;

import java.util.List;

/**
 * One kind of structure the plugin manages (so far the Ocean Monument). The main class starts every
 * module, gives it its /structure subcommand and collects its status and command help for
 * status.txt and commands.txt.
 */
public interface StructureModule {

    /** Shown as the heading in /structure ... status, status.txt and commands.txt, e.g. "Ocean Monument". */
    String name();

    /** The word after /structure, e.g. "monument". */
    String commandName();

    StructureCommand.Subcommand command();

    /** The structure's current status, one line per fact. */
    List<StatusLine> status();

    /** Every command of this structure, for commands.txt. */
    List<CommandHelp> commandHelp();

    void enable();

    void disable();
}
