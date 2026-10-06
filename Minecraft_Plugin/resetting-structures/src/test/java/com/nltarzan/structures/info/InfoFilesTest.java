package com.nltarzan.structures.info;

import com.nltarzan.structures.info.InfoFiles.CommandGroup;
import com.nltarzan.structures.info.InfoFiles.StatusSection;
import com.nltarzan.structures.monument.command.MonumentCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoFilesTest {

    @Test
    void statusFileHasAHeaderAndOneSectionPerStructure() {
        String text = InfoFiles.statusText(List.of(
                new StatusSection("Ocean Monument", List.of(
                        new StatusLine("Location", "world 224 40 -320", NamedTextColor.WHITE),
                        new StatusLine("Elder Guardians", "3 alive", NamedTextColor.RED))),
                new StatusSection("Woodland Mansion", List.of(
                        new StatusLine("Location", "world 0 70 0", NamedTextColor.WHITE)))),
                LocalDateTime.of(2026, 10, 6, 14, 3, 12));

        assertTrue(text.contains("READ-ONLY"));
        assertTrue(text.contains("Updated 2026-10-06 14:03:12"));
        assertTrue(text.contains("\nOcean Monument\n"));
        assertTrue(text.contains("\nWoodland Mansion\n"));
        // Labels in one section are padded to the same width, so the values line up.
        assertTrue(text.contains("  Location:        world 224 40 -320\n"), text);
        assertTrue(text.contains("  Elder Guardians: 3 alive\n"), text);
    }

    @Test
    void commandsFileListsEveryMonumentCommand() {
        String text = InfoFiles.commandsText(List.of(new CommandGroup("Ocean Monument", "monument", MonumentCommand.HELP)));

        assertTrue(text.contains("READ-ONLY"));
        assertTrue(text.contains("structures_admin"));
        assertTrue(text.contains("Ocean Monument   (/structure monument ...)"));
        for (CommandHelp command : MonumentCommand.HELP) {
            assertTrue(text.contains("\n" + command.usage() + "\n"), "missing " + command.usage());
            for (String example : command.examples()) {
                assertTrue(text.contains("    Example: " + example + "\n"), "missing example " + example);
            }
        }
        for (String line : text.split("\n")) {
            assertTrue(line.length() <= 90, "line too long: " + line);
        }
    }
}
