package com.nltarzan.races.util;

public final class Times {

    private Times() {
    }

    /** Formats milliseconds as 1:23.456, or 23.456 when under a minute. */
    public static String format(long ms) {
        long minutes = ms / 60_000;
        long seconds = (ms / 1000) % 60;
        long millis = ms % 1000;
        return minutes > 0
                ? String.format("%d:%02d.%03d", minutes, seconds, millis)
                : String.format("%d.%03d", seconds, millis);
    }

    /** Formats whole seconds as 5:00 or 0:45. */
    public static String formatSeconds(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
