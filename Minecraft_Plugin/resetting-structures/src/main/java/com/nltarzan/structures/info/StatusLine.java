package com.nltarzan.structures.info;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * One fact in a structure's status, e.g. "Sponges: 87". Shown in color by the status command and
 * as plain text in status.txt.
 */
public record StatusLine(String label, String value, NamedTextColor color) {
}
