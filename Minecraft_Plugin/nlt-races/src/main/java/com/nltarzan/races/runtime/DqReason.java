package com.nltarzan.races.runtime;

import java.util.Locale;

/** Why a racer was disqualified. The key is stored in runs.dq_reason and names the message play.dq.&lt;key&gt;. */
public enum DqReason {
    TELEPORT,
    ENDER_PEARL,
    CHORUS_FRUIT,
    FLYING,
    GAMEMODE,
    DEATH,
    LOGOUT,
    WORLD_CHANGE,
    /** Foot races: mounting any entity or vehicle. */
    FOOT_MOUNT,
    /** Foot races: gliding with an elytra. */
    FOOT_GLIDE,
    /** Vehicle races, strict rules: got off the vehicle. */
    DISMOUNT,
    /** Vehicle races, lenient rules: didn't get back on within remount-time. */
    REMOUNT_TIMEOUT,
    /** Vehicle races: the vehicle disappeared (e.g. a minecart or boat was destroyed). */
    VEHICLE_LOST,
    /** Elytra races, strict rules: stopped gliding. */
    LANDED,
    /** Elytra races, lenient rules: didn't take off again within remount-time. */
    TAKEOFF_TIMEOUT,
    /** Elytra races: never took off within takeoff-time after the rules started. */
    NO_TAKEOFF;

    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
