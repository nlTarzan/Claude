package com.nltarzan.structures.util;

import java.util.Locale;

/**
 * Durations written like the datapack's {@code /schedule} times: {@code 5t}, {@code 10s}, {@code 2d}.
 * A plain number is ticks.
 */
public final class Durations {

    public static final long TICKS_PER_SECOND = 20;
    public static final long TICKS_PER_DAY = 24_000;

    private Durations() {
    }

    public static long parseTicks(String text) {
        String value = text.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty duration");
        }

        char unit = value.charAt(value.length() - 1);
        long multiplier = 1;
        String number = value;
        if (Character.isLetter(unit)) {
            multiplier = switch (unit) {
                case 't' -> 1;
                case 's' -> TICKS_PER_SECOND;
                case 'd' -> TICKS_PER_DAY;
                default -> throw new IllegalArgumentException(
                        "Unknown unit '" + unit + "' in '" + text + "', use t, s or d");
            };
            number = value.substring(0, value.length() - 1);
        }

        double amount;
        try {
            amount = Double.parseDouble(number);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration '" + text + "', use e.g. 20t, 10s or 2d");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Duration must be positive: '" + text + "'");
        }
        return Math.max(1, Math.round(amount * multiplier));
    }

    /** Human readable real time for a number of ticks, e.g. {@code 1h 4m 30s}. */
    public static String format(long ticks) {
        long seconds = Math.max(0, ticks) / TICKS_PER_SECOND;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder out = new StringBuilder();
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            out.append(minutes).append("m ");
        }
        return out.append(secs).append('s').toString();
    }
}
