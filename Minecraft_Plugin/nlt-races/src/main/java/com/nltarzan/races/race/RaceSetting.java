package com.nltarzan.races.race;

import java.util.Locale;

/** Number settings stored under {@code settings:} in each race's yml. */
public enum RaceSetting {
    /** Seconds of 3-2-1 before a group or points race starts. */
    COUNTDOWN("countdown", 3, 1, 30),
    /** Lenient rules: seconds to remount (vehicle) or take off again (elytra). */
    REMOUNT_TIME("remount-time", 5, 1, 60),
    /** Solo: seconds before an unfinished run becomes DNF. */
    MAX_TIME("max-time", 600, 10, 86_400),
    /** Group: seconds after the first finisher before everyone still racing is DNF. */
    DNF_AFTER_FIRST_FINISH("dnf-after-first-finish", 60, 0, 3_600),
    /** Points: game length in seconds. */
    TIME_LIMIT("time-limit", 300, 10, 86_400),
    /** Points: the game ends as soon as someone reaches this many points. */
    POINTS_TO_WIN("points-to-win", 10, 1, 1_000),
    /** Group/points: players needed (leader included) before the leader can start. */
    MIN_PLAYERS("min-players", 2, 1, 100),
    /** Group/points: seconds after prep before the race cancels itself if the leader hasn't started it. */
    PREP_TIMEOUT("prep-timeout", 300, 30, 3_600),
    /**
     * Elytra: seconds racers may stay on the ground after the elytra rules start (GO in points races, the
     * start checkpoint otherwise) before they must have taken off.
     */
    TAKEOFF_TIME("takeoff-time", 10, 1, 120);

    private final String key;
    private final int defaultValue;
    private final int min;
    private final int max;

    RaceSetting(String key, int defaultValue, int min, int max) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public boolean appliesTo(Race race) {
        return switch (this) {
            case COUNTDOWN, MIN_PLAYERS, PREP_TIMEOUT -> race.getFormat().isMultiplayer();
            case REMOUNT_TIME -> race.getRules() == Rules.LENIENT && race.getTravel().hasRules();
            case MAX_TIME -> race.getFormat() == RaceFormat.SOLO;
            case DNF_AFTER_FIRST_FINISH -> race.getFormat() == RaceFormat.GROUP;
            case TIME_LIMIT, POINTS_TO_WIN -> race.getFormat() == RaceFormat.POINTS;
            case TAKEOFF_TIME -> race.getTravel() == TravelMode.ELYTRA;
        };
    }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static RaceSetting byKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (RaceSetting setting : values()) {
            if (setting.key.equals(lower)) {
                return setting;
            }
        }
        return null;
    }

    public String key() {
        return key;
    }

    public int defaultValue() {
        return defaultValue;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}
