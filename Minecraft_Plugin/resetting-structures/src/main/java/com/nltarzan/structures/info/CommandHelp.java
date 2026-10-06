package com.nltarzan.structures.info;

import java.util.List;

/**
 * One command for commands.txt.
 *
 * @param usage       the command as typed, e.g. "/structure monument reset generated [seed]"
 * @param description what it does
 * @param examples    example uses, may be empty
 */
public record CommandHelp(String usage, String description, List<String> examples) {

    public CommandHelp {
        examples = List.copyOf(examples);
    }

    public CommandHelp(String usage, String description) {
        this(usage, description, List.of());
    }
}
