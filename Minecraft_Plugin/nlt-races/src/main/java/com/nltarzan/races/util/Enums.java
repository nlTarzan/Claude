package com.nltarzan.races.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Case-insensitive enum parsing; "happy-ghast" and "HAPPY_GHAST" both match HAPPY_GHAST. */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String input) {
        if (input == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, input.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** How a constant is shown to players and typed in commands, e.g. "happy-ghast". */
    public static String display(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static List<String> options(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enums::display).toList();
    }
}
