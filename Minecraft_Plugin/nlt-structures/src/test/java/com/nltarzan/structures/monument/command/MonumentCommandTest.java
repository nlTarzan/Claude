package com.nltarzan.structures.monument.command;

import com.nltarzan.structures.info.CommandHelp;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps commands.txt (MonumentCommand.HELP) in step with the actual subcommands. */
class MonumentCommandTest {

    @Test
    void everySubcommandIsExplainedAndNothingElse() {
        Set<String> explained = MonumentCommand.HELP.stream()
                .map(MonumentCommandTest::subcommandOf)
                .collect(Collectors.toSet());
        assertEquals(Set.copyOf(MonumentCommand.SUBCOMMANDS), explained,
                "MonumentCommand.HELP must explain exactly the subcommands in SUBCOMMANDS");
    }

    @Test
    void everyHelpEntryHasAnExplanationAndValidExamples() {
        for (CommandHelp help : MonumentCommand.HELP) {
            assertTrue(help.usage().startsWith(MonumentCommand.COMMAND + " "), help.usage());
            assertFalse(help.description().isBlank(), help.usage() + " has no explanation");
            for (String example : help.examples()) {
                assertEquals(subcommandOf(help), example.split(" ")[2], "example for the wrong command: " + example);
            }
        }
    }

    /** "/nltstructures monument [status]" -> "status", "/nltstructures monument reset <variant>" -> "reset". */
    private static String subcommandOf(CommandHelp help) {
        return help.usage().split(" ")[2].replace("[", "").replace("]", "");
    }
}
