package com.nltarzan.races.race;

public enum TravelMode {
    FOOT,
    ELYTRA,
    /** Uses a {@link VehicleType} spawned by the plugin. */
    VEHICLE,
    /** No travel restrictions. */
    FREE;

    /** Travel modes where strict/lenient rules make a difference. */
    public boolean hasRules() {
        return this == ELYTRA || this == VEHICLE;
    }
}
